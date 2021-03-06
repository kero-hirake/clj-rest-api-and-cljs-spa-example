(ns todo-app.routes
  (:require [accountant.core :as accountant]
            [bidi.bidi :as bidi]))

(def routes
  ["/" {"" :todo-app.views/home
        "list" :todo-app.views/list
        "create" :todo-app.views/create
        [[#"\d+" :id] "/edit"] :todo-app.views/edit ;<-match-routeは値が帰ってくるが、何も表示されな
        "edit" :todo-app.views/edit}])

(defn navigate [view]
  (accountant/navigate! (bidi/path-for routes view)))
