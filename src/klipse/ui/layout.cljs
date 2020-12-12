(ns klipse.ui.layout
  (:require
   [klipse.control.control :refer [app-state]]
   [klipse.ui.editors.cljs :refer [cljs-editor]]
   [klipse.ui.editors.js :refer [js-editor]]
   [klipse.ui.outputs.container :refer [container]]
   [klipse.ui.outputs.cljs :refer [cljs-textarea]]
   [klipse.ui.outputs.js :refer [js-textarea]]))

(defn layout []
  (case (:code-layout @app-state)
    :eval-only
    [:div {:className "klipse-layout klipse-layout-eval-only"}
     [:div  {:className "klipse-item"}
      [cljs-editor]]
     [:div  {:className "klipse-item"}
      [cljs-textarea]]
     [:div  {:className "klipse-item"}
      [js-textarea]]]

    :js-only
    [:div {:className "klipse-layout klipse-layout-js-only"}
     [:div {:className "klipse-item"}
      [cljs-editor]]
     [:div #js {:className "klipse-item"}
      [js-editor]]]

    :with-container
    [:div {:className "klipse-layout klipse-layout-global"}
     [:div {:className "klipse-item"}
      [cljs-editor]]
     [:div {:className "klipse-item"}
      [container]]
     [:div {:className "klipse-item"}
      [cljs-textarea]]
     [:div {:className "klipse-item"}
      [js-textarea]]]

    [:div {:className "klipse-layout klipse-layout-global"}
     [:div {:className "klipse-item"}
      [cljs-editor]]
     [:div {:className "klipse-item"}
      [js-editor]]
     [:div {:className "klipse-item"}
      [cljs-textarea]]
     [:div {:className "klipse-item"}
      [js-textarea]]]))
