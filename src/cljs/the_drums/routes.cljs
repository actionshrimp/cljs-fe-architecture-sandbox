(ns the-drums.routes)

(def routes
  ["/" {"" :main
        "index.html" :main}
   ["/user/" :id] :user])
