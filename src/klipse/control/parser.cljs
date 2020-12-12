(ns klipse.control.parser
  (:require [cljs.reader :refer [read-string]]
            [klipse.utils :refer [url-parameters verbose?]]
            [cljs.core.async :refer [<!]]
            [klipse-clj.lang.clojure :refer [compile-async eval-async-map]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

;; =============================================================================
;; Utils
(defn safe-read-string [s]
  (try (read-string s)
       (catch js/Object _
         s)))

(defn static-fns? []
  (boolean (safe-read-string (or (:static-fns (url-parameters)) "false"))))

(defn compile-display-guard? []
  (boolean (safe-read-string (or (:compile-display-guard (url-parameters)) "false"))))

(defn beautify-strings? []
  (boolean (safe-read-string (or (:beautify-strings (url-parameters)) "false"))))

(defn eval-context? []
  (keyword (safe-read-string (or (:eval-context (url-parameters)) "nil"))))


(defn external-libs
  "Collection of paths to look for external dependencies.
  The external libs must be a vector of strings e.g. [\"https://raw.githubusercontent.com/vvvvalvalval/scope-capture/master/src\"]
  See for instance https://raw.githubusercontent.com/vvvvalvalval/scope-capture/master/src"
  []
  (safe-read-string (or (:external-libs (url-parameters)) "[]")))

(defn max-eval-duration []
  (safe-read-string (or (:max-eval-duration (url-parameters) "nil"))))

(defn print-length []
  (safe-read-string (or (:print-length (url-parameters)) "1000")))

(defn eval-clj [s]
  (go
    (let [{:keys [warnings res]} (<! (eval-async-map s {:static-fns (static-fns?)
                                                        :verbose (verbose?)
                                                        :beautify-strings (beautify-strings?)
                                                        :external-libs (external-libs)
                                                        :max-eval-duration (max-eval-duration)
                                                        :print-length (print-length)
                                                        :context (eval-context?)}))
          [status result] res]
      [status (str warnings result)])))


(defn save-editor-input! [state value]
  (swap! state assoc-in [:input :input] value))

(defn consume-editor-mode! [state value]
  (swap! state update-in [:input :editor-modes] rest)
  (swap! state assoc-in [:input :editor-mode] value))

(defn set-editor-mode! [state value]
  (swap! state assoc-in [:input :editor-mode] value))

(defn clean-print-box! [state]
  (swap! state assoc :evaluation-js ""))

(defn append-print-box! [state & args]
  (swap! state update :evaluation-js #(str % (apply str args))))

(defn eval-and-compile! [state value]
  {:action (go
              (clean-print-box! state)
              (binding [*print-newline* true
                        *print-fn* (partial append-print-box! state)]
                (swap! state assoc
                       :evaluation-clj (<! (eval-clj value))
                       ;; we need to prevent from evaluation and compilation to occurs in paralllel - as it would load twice the code of the deps
                       :compilation (<! (compile-async value {:static-fns (static-fns?)
                                                              :verbose (verbose?)
                                                              :external-libs (external-libs)
                                                              :compile-display-guard (compile-display-guard?)
                                                              :max-eval-duration (max-eval-duration)
                                                              :context (eval-context?)})))))})
