(ns klipse.app
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    ;require codemirror addons here - as in the plugin they are loaded dynamically
    cljsjs.codemirror.mode.clojure
    cljsjs.codemirror.mode.javascript
    cljsjs.codemirror.addon.edit.matchbrackets
    cljsjs.codemirror.addon.edit.closebrackets
    cljsjs.codemirror.addon.display.placeholder
    cljsjs.codemirror.addon.scroll.simplescrollbars
    [reagent.dom :as rd]
    [cljs.core.async :refer [<!]]
    [klipse.ui.layout :as ui]
    [klipse.utils :refer [read-input-from-gist gist-path-page url-parameters]]
    [klipse.control.control :as control]))


(defn read-input-from-url []
  (:cljs_in (url-parameters)))

(defn gist-content [gist-id]
  (go
    (let [gist-intro (str "loaded from gist: " (gist-path-page gist-id))
          gist-content (<! (read-input-from-gist gist-id))]
      (str ";" gist-intro "\n" gist-content))))

(defn read-src-input []
  (go
    (or
      (read-input-from-url)
      (when-let [gist-id (:cljs_in.gist (url-parameters))]
        (<! (gist-content gist-id))))))

(defn init [element]
  (go
    (let [input (<! (read-src-input))]
      (control/init-state! input)
      (rd/render [ui/layout] element))))
