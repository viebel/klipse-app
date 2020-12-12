(ns klipse.ui.editors.js
  (:require
   [reagent.core :as r]
   [klipse.control.control :refer [app-state]]
   [klipse.ui.editors.editor :as editor]))

(def config-editor
  {:lineNumbers true
   :matchBrackets true
   :lineWrapping true
   :autoCloseBrackets true
   :mode "javascript"
   :readOnly true
   :scrollbarStyle "overlay"})

(def placeholder-editor
  ";; Press Ctrl-Enter or wait for 3 sec to transpile...")

(def editor-id "code-js")

(defn display-editor? [code-layout]
  (#{:global :js-only} code-layout))

(defn init-editor! [id]
  (editor/create id config-editor))

(def the-editor (atom nil))

(defn on-component-update [_props _prev-props _prev-state]
  (when (display-editor? (:code-layout @app-state))
    (let [[status result] (:compilation @app-state)]
      (->>
       (if (= :ok status) result (str result))
       (editor/set-value @the-editor)
       (editor/do-indent)))))

(defn js-editor []
  (r/create-class
   {:component-did-mount (fn []
                           (reset! the-editor (init-editor! editor-id)))
    :component-did-update on-component-update
    :reagent-render (fn []
                      [:section {:className "js-editor"}
                       [:div {:className "totally-hidden"} ;; refers app-state ratom so that comp is re-rendered
                        (:code-layout @app-state)]
                       [:textarea {:id editor-id
                                   :placeholder placeholder-editor}]])}))
