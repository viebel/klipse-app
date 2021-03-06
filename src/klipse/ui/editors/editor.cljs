(ns klipse.ui.editors.editor
  (:use-macros
   [gadjett.core :only [dbg]])
  (:require
   [goog.dom :as gdom]
   [klipse.dom-utils :refer [create-div-after add-class]]
   [gadjett.collections :as gadjett]
   cljsjs.codemirror
   cljsjs.codemirror.addon.edit.matchbrackets
   cljsjs.codemirror.addon.edit.closebrackets
   cljsjs.codemirror.addon.hint.show-hint
   [clojure.string :refer [blank?]]
   [applied-science.js-interop :as j]))

(def code-mirror js/CodeMirror)

(defn create [dom-id config]
  (js/CodeMirror.fromTextArea
    (js/document.getElementById dom-id)
    (clj->js config)))

(defn get-value [editor]
  (.getValue editor))

(defn get-selection [editor]
  (.getSelection editor))

(defn get-selection-or-nil [editor]
  (let [s (get-selection editor)]
    (if (blank? s)
      nil
      s)))

(defn get-selection-when-selected [editor]
  (or (get-selection-or-nil editor)
      (get-value editor)))

(defn set-value [editor value] 
  (.setValue editor value)
  editor)

(defn on-change [editor f]
  (.on editor "change" f)
  editor)

(defn set-option [editor option value]
  (.setOption editor option value)
  editor)

(defn fix-blank-lines [editor]
  (->> (get-value editor)
      gadjett/fix-blank-lines
      (set-value editor)))

(defmulti beautify-language (fn [editor mode] mode))

(defmethod beautify-language :default [editor _] editor)


(defn fix-comments-lines [editor mode]
  (if (= "clojure" mode)
    (->> (get-value editor)
         gadjett/remove-ending-comments
         (set-value editor))
    editor))

(defn do-indent [editor]
  (j/call editor :operation #(dotimes [line (j/call editor :lineCount)]
                               (j/call editor :indentLine line "smart")))
  editor)

(defn beautify [editor mode {:keys [indent? remove-ending-comments?]}]
  (as-> editor $
      (if indent? (do-indent $) $)
      (fix-blank-lines $)
      (if remove-ending-comments? (fix-comments-lines $ mode) $)
      (beautify-language $ mode)))

(defn set-value-and-beautify [editor mode value opts]
  (-> (set-value editor value)
      (beautify mode opts)))

(defn list-completions [completions editor]
  (let [cursor (.getCursor editor)
        token (.getTokenAt editor cursor)
        start (.-start token)
        end (.-ch cursor)
        line (.-line cursor)]
    (clj->js {:list (rest completions)
              :from (js/CodeMirror.Pos line start)
              :to   (js/CodeMirror.Pos line end)})))

(defn current-token [editor]
  (let [cursor (.getCursor editor)
        token (.getTokenAt editor cursor)]
    (.-string token)))

(defn trigger-autocomplete [editor completions]
  (let [hint-fn (partial list-completions completions)]
    (js/setTimeout
      (fn []
        (.showHint editor (clj->js {:hint           hint-fn
                                    :completeSingle true})))
      100)))

(defn replace-element-by-editor [element value {:keys [mode] :as opts} & {:keys [klass indent? remove-ending-comments?] :or {indent? true remove-ending-comments? true}}]
  (let [editor (js/CodeMirror (fn [elt]
                                (if-not klass
                                  (gdom/replaceNode elt element)
                                  (let [wrapping-div (gdom/createElement "div")]
                                    (gdom/appendChild wrapping-div elt)
                                    (gdom/replaceNode wrapping-div element)
                                    (add-class wrapping-div klass))))
                              (clj->js opts))]
    (-> (set-value editor value)
        (beautify mode {:indent? indent? :remove-ending-comments? remove-ending-comments?}))))

(defn replace-id-by-editor [id cm-opts & more-opts]
  (let [element (gdom/getElement id)
        value  (aget element "textContent")]
    (apply replace-element-by-editor element value cm-opts more-opts)))

(defn create-editor-after-element [element value opts & {:keys [klass remove-ending-comments? indent?] :or  {remove-ending-comments? false indent? false}}]
  (-> (create-div-after element {})
      (replace-element-by-editor value opts :remove-ending-comments? remove-ending-comments? :indent? indent? :klass klass)))
