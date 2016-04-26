(ns the-drums.style-guide
  (:require [reagent.core :as r]
            [cljs.core.async :as a]
            [the-drums.core :as c])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go go-loop]]))

(defn top-stores [stores]
  [:ul
   (doall
    (for [s stores]
      [:li {:key (:store-id s)} (:store-name s)]))])

(defn user-pod [user send-event!]
  (let [send-follow-event! #(send-event! [:user-pod-follow (:user-id user)])
        send-view-event! #(send-event [:view-user (:user-id user)])]
    (fn []
      [:div.user-pod
       [:a {:href "#"
            :on-click send-view-event!}
        [:div.pic {:style {:background-color (:profile-pic user)}}
         "pic"]]
       [:div.side
        [:div.username (:username user)]
        [:button.follow {:on-click send-follow-event!}
         "Follow!"]]])))

(defn style-guide []
  (let [test-send! (fn [e] (js/console.log (pr-str e)))]
    [:div
     (let [input {:username "Daaave"
                  :profile-pic :red
                  :user-id 123}

           test-send-scoped! #(test-send! [:user-pod %])]
       [:div.card
        [:h2 "User pod"]
        [:div.card-body
         [:div.card-body-input (pr-str input)]
         [:div.card-body-render
          [user-pod input test-send-scoped!]]]])

     (let [input [{:store-id 1 :store-name "SUPASTORE"}
                  {:store-id 2 :store-name "BOOMSTORE"}
                  {:store-id 3 :store-name "STORESTORE"}]

           test-send-scoped! #(test-send! [:top-stores %])]

       [:div.card
        [:h2 "Top stores"]
        [:div.card-body
         [:div.card-body-input (pr-str input)]
         [:div.card-body-render
          [top-stores input test-send-scoped!]]]])])
  )

(defn init []
  (r/render-component [style-guide]
                      (.getElementById js/document "container")))
