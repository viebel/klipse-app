# klipse-app
Clojure[script] REPL online

```bash
clojure -A:figwheel --build dev --repl
```


In order to launch a cljs repl in Cider, we have to provide `-A:fig-cider` trough jack-in params. (You need to enable `cider-edit-jack-in-command` in emacs). But this is done autmagically by the variables defined in `.dirs-locals.el`.

