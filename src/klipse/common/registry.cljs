(ns klipse.common.registry
  (:require
   [klipse.utils :refer [klipse-settings]])
  (:require-macros
   [gadjett.core :refer [dbg]]))

(def selector->mode (atom {}))
(def mode-options (atom {}))

(defn codemirror-mode-src [mode]
  (let [root (:codemirror_root (klipse-settings) "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.21.0/mode")]
    (str root "/" mode "/" mode ".min.js")))

(defn codemirror-keymap-src [mode]
  (let [root (:codemirror_root (klipse-settings) "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.21.0/keymap")]
    (str root "/" mode ".min.js")))

(defn scripts-src [name]
  (let [root (:scripts_root (klipse-settings) "https://viebel.github.io/klipse/repo/js")]
    (str root "/" name)))
