(ns klipse.ui.editors.cljs
  (:require-macros
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [reagent.core :as r]
   [clojure.string :as string :refer [blank?]]
   [parinfer-codemirror.editor :refer [parinferize-and-sync!]]
   [klipse-clj.lang.clojure :refer [completions]]
   [klipse.common.registry :refer [codemirror-keymap-src scripts-src]]
   [klipse.ui.editors.editor :refer [trigger-autocomplete current-token get-selection-when-selected get-value replace-element-by-editor replace-id-by-editor]]
   [klipse.ui.editors.common :refer [handle-events]]
   [klipse.utils :refer [load-scripts-mem]]
   [klipse.control.parser :refer [eval-and-compile! set-editor-mode!
                                  save-editor-input! consume-editor-mode!]]
   [klipse.control.control :refer [app-state]]))

(def config-editor
  {:lineNumbers true
   :lineWrapping true
   :matchBrackets true
   :autoCloseBrackets true
   :mode "clojure"
   :scrollbarStyle "overlay"})

(def editor-id "code-cljs")
(def my-editor (atom nil))

(def placeholder-editor
  (str
   ";; Write your clojurescript expression \n"
   ";; and press Ctrl-Enter or wait for 3 sec to experience the magic..."))

(defn save-input! [s]
  (when-not (blank? s)
    (save-editor-input! app-state s)))

(defn process-input! [s]
  (when-not (blank? s)
    (eval-and-compile! app-state s)))

(defn show-completions [s]
  (js/console.log (clj->js (completions s))))

(defn handle-cm-events [editor]
  (set! js/CCC editor)
  (handle-events editor
                 {:idle-msec 3000
                  :on-change #(save-input!  (get-value editor))
                  :on-completion #(trigger-autocomplete editor (completions (current-token editor)))
                  :on-should-eval #(process-input! (get-selection-when-selected editor))}))

(defmulti use-editor-mode! (fn [mode]  mode))

(defn replace-editor! [& [cm-options]]
  (let [editor @my-editor
        editor-wrapper (.getWrapperElement editor)
        value (get-value editor)
        new-editor (replace-element-by-editor editor-wrapper value (merge config-editor cm-options))]
    (reset! my-editor new-editor)
    (handle-cm-events new-editor)))

(defn load-external-scripts [scripts]
  (go
    (let [[status http-status script] (<! (load-scripts-mem scripts))]
      (if (= :ok status)
        [:ok :ok]
        [:error (str "Cannot load script: " script "\n" "Error: " http-status)]))))

(def parinfer-count (atom 0))

(defn parinferize-editor! [editor indent-or-paren]
  (let [key- (swap! parinfer-count inc)
        wrapper (.getWrapperElement editor)
        parinfer-mode (case indent-or-paren
                        :indent :indent-mode
                        :paren :paren-mode)]
    (set! (.-id wrapper) (str "cm-" "element-id"))
    (parinferize-and-sync! editor key- parinfer-mode (get-value editor))))

(defn use-parinfer! [indent-or-paren]
  (parinferize-editor! (replace-editor!) indent-or-paren)
  (let [mode (case indent-or-paren
               :indent :parinfer-indent
               :paren :parinfer-paren)]
    (consume-editor-mode! app-state mode)))

(defmethod use-editor-mode! :parinfer-indent [_]
  (use-parinfer! :indent))

(defmethod use-editor-mode! :parinfer-paren [_]
  (use-parinfer!  :paren))

(defmethod use-editor-mode! :paredit [_]
  (consume-editor-mode! app-state :loading)
  (go
    (let [[status err] (<! (load-external-scripts [(codemirror-keymap-src "emacs") (scripts-src "subpar.js") (scripts-src "subpar.core.js")]))]
      (if (= :ok status)
        (do
          (replace-editor! {:keyMap "subpar"})
          (set-editor-mode! app-state :paredit))
        (do
          (set-editor-mode! app-state :error)
          (js/console.error "cannot load paredit scripts:" err))))))

(defmethod use-editor-mode! :regular [_]
  (let [editor (replace-editor!)]
    (consume-editor-mode! app-state :regular)
    editor))

(defn switch-editor-mode! []
  (let [editor-modes (get-in @app-state [:input :editor-modes])]
    (use-editor-mode! (first editor-modes))))

(defn init-editor! [id]
  (replace-id-by-editor id config-editor))

(defn on-component-mount []
  (reset! my-editor (init-editor! editor-id))
  (switch-editor-mode!)
  (process-input! (get-value @my-editor)))

(defn render []
  (let [{{:keys [input editor-mode]} :input} @app-state
        editor-class (case editor-mode
                       :loading "mode-loading"
                       :error "mode-error"
                       :regular "mode-regular"
                       :paredit "mode-paredit"
                       :parinfer-paren "mode-parinfer-paren"
                       :parinfer-indent "mode-parinfer-indent"
                       "mode-regular")]
    [:section {:className "cljs-editor"}
     [:div {:autoFocus true
            :id editor-id
            :placeholder placeholder-editor}
      input]
     [:div {:onClick switch-editor-mode!
            :className (str "editor-logo" " " editor-class)}]]))

(defn cljs-editor []
  (r/create-class
   {:component-did-mount on-component-mount
    :reagent-render render}))
