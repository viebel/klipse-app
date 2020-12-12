(ns klipse.ui.outputs.container
  (:require
   [reagent.core :as r]
   [reagent.dom :refer [dom-node]]))

(defn container-inner []
  (r/create-class
   {
    :component-did-mount (fn [this]
                           (set! js/klipse-container (dom-node this)))
    :should-component-update (constantly false)

    :reagent-render (fn []
                      [:div {:id "klipse-container"}
                       [:p "This is your "
                        [:strong "klipse container"]
                        "."]
                       [:p "You can access it with "
                        [:code "(js/document.getElementById \"klipse-container\")"]
                        " or with " [:code "js/klipse-container"]
                        "."]
                       [:p "For instance, try to copy and paste the following code into the top left box and press 'Ctrl-Enter` or wait for 3 seconds:"]
                       [:pre
                        [:code
                         "(set!\n (.-innerHTML js/klipse-container)\n \"<div style='color:blue;'> Hello <b>World</b>!</div>\")"]]])}))


(defn container []
  (r/create-class
   {:should-component-update (constantly false)
    :reagent-render (fn []
                      [:div {:id "klipse-container-wrapper"}
                       [container-inner]])}))
