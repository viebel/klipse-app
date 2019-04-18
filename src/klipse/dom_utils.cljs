(ns klipse.dom-utils
  (:require
   [goog.dom :as gdom]
   [applied-science.js-interop :as j]))


(defn add-class [element klass]
  (j/call-in element [:classList :add] klass))

(defn create-div-after [element attrs]
    (let [div (gdom/createDom "div" (clj->js attrs) (gdom/createTextNode ""))]
      (gdom/insertSiblingAfter div element)
      div))

