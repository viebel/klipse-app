(ns klipse.utils
  (:require-macros
   [gadjett.core :refer [dbg]]
   [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [cljs.reader :refer [read-string]]
   [clojure.walk :refer [keywordize-keys]]
   [cljs-http.client :as http]
   [cljs.core.async :refer [timeout <! chan put!]]
   [cemerick.url :refer [url]]))

(defn show-url
[url]
  (
    (js/alert url)
    (print url)
  )
)

(defn fetch-shortened-url
  "Calls is.gd with the current URL and returns
  a shortened version for the user to copy."
  [current]
  (go (let [response (<! (http/get
      "https://cors-anywhere.herokuapp.com/https://is.gd/create.php"
      {:with-credentials? false
      :headers {"accept" "application/json" "content-type" "application/json"}
      :query-params {:format "simple" :url current}}))]
      (if-not (= (:status response) 200)
        (show-url current)
        (let [short-link (:body response)]
          (show-url short-link)
        )
      )
  ))
)

(defn current-url []
  (if-let [loc (if-not (nil? js/location) js/location "")]
    (url (aget loc "href"))))

(defn url-parameters* []
  (-> (current-url)
      :query
      keywordize-keys))

(def url-parameters (memoize url-parameters*))

(defn add-url-parameter
  "Returns the current url with an additional parameter.
  If the parameter already exists, it is overridden."
  [base-url key value]
  (-> base-url
      (assoc-in [:query (name key)] value)
      str))

(defn process-url [base-url input]
  (let [a (add-url-parameter base-url :cljs_in input)]
  (fetch-shortened-url a)
  )
)

(defn create-url-with-input [base-url input]
  (->
    (if base-url (url base-url) (current-url))
    (process-url input)
  )
)

(defn debounce [func wait-in-ms]
  (let [counter (atom 0)]
    [(fn [] ; a function that will execute `func` after a while if the counter is 0
       (go
         (swap! counter inc)
         (<! (timeout wait-in-ms))
         (swap! counter dec)
         (when (zero? @counter)
           (func))))
     (fn [] ; a function that executes `func` immediately
       (go
         (func)
         (swap! counter inc)
         (<! (timeout wait-in-ms))
         (swap! counter dec)))]))

(defn gist-path-raw [gist-id]
  (str "https://gist.githubusercontent.com/" gist-id "/raw" "?" (rand)))

(defn gist-path-page [gist-id]
  (str "https://gist.github.com/" gist-id))

(defn read-input-from-gist [gist-id]
  (go
    (when gist-id
      (let [gist-url (gist-path-raw gist-id)
            {:keys [status body]} (<! (http/get
                                       gist-url
                                       {:with-credentials? false}))]
        (if-not (= status 200)
          (str "\""
               "Wrong gist path: " gist-url "\n"
               "gist-id= " gist-id "\n"
               "http status: " status
               "\"")
          body)))))

(defn runonce [f]
  (let [ran (atom false)]
    (fn [& args]
      (when-not @ran
        (reset! ran true)
        (apply f args)))))


(defn memoize-async
  "Returns a memoized version of f.
  If `f` succeeds (returns [:ok & args]), on subsequent calls it will return the cached results.
  `f` must return a channel."
  [f]
  (let [ran (atom {})]
    (fn [& args]
      (go
        (if-not (contains? @ran args)
          (let [res (<! (apply f args))]
            (when (= :ok (first res))
              (swap! ran assoc args res))
            res)
          (get @ran args))))))


(def eval-in-global-scope js/eval) ; this is the trick to make `eval` work in the global scope: http://perfectionkills.com/global-eval-what-are-the-options/
                                   ; if we make it a function (defn eval-in-global-scope[x] (js/eval x)) - code is not shared properly between javascript snippets - see https://github.com/viebel/klipse/issues/246#issue-214278867


(defn load-script [script & _]
  (go
    (js/console.info "loading:" script)
    (let [{:keys [status body]} (<! (http/get script {:with-credentials? false}))]
      (if (= 200 status)
        (do
          (js/console.info "evaluating:" script)
          (eval-in-global-scope body)
          (js/console.info "evaluation done:" script)
          [:ok script])
        [status script]))))

(def load-script-mem (memoize-async load-script))

(defn load-scripts [scripts & _]
  (go-loop [the-scripts scripts]
    (if (seq the-scripts)
      (let [script (str (first the-scripts))
            [status script] (<! (load-script-mem script :secured-eval? false))]
        (if (= :ok status)
          (recur (rest the-scripts)))
        [status script])
      [:ok])))

(def load-scripts-mem (memoize-async load-scripts))

(defn verbose? []
  (boolean (read-string (or (:verbose (url-parameters)) "false"))))

(defn klipse-settings* []
  (let [w (if-not (nil? js/window) js/window #js {})]
    (->
      (aget w "klipse_settings")
      (js->clj :keywordize-keys true))))

(def klipse-settings (memoize klipse-settings*))

