(ns klipse.ui.outputs.cljs
  (:require
   [klipse.control.control :refer [app-state]]))

(def placeholder-textarea
  ";; Press Ctrl-Enter or wait for 3 sec to eval in clojure...")

(defn cljs-textarea []
  (let [[status result] (:evaluation-clj @app-state)
        status-class (when status (name status))]
    [:section {:className "cljs-textarea"}
     [:textarea {:value (or result "")
                 :className status-class
                 :placeholder placeholder-textarea
                 :readOnly true}]]))
