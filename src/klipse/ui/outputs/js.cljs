(ns klipse.ui.outputs.js
  (:require
   [klipse.control.control :refer [app-state]]))

(def placeholder-textarea ";; Here you will see what you print in your code...")

(defn js-textarea []
  (let [value (:evaluation-js @app-state)]
    [:section {:className "js-textarea"}
     [:textarea {:value (or value "")
                 :placeholder placeholder-textarea
                 :className "ok"
                 :readOnly true}]]))
