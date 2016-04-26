(ns the-drums.user-page
  (:require [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :as a]
            [cljs.core.match :refer-macros [match]]
            [the-drums.core :as c]
            [the-drums.style-guide :as sg]
            [the-drums.util :as util]
            [the-drums.navigation :as nav])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go go-loop]]))

(defn send-page-event! [e] (c/send-event! [:user-page e]))
(def search-chan (a/chan (a/sliding-buffer 1)))

(defn <lookup-username [username]
  (let [c (http/get (str "https://api.socialsuperstore.com/v1/users/by-username/" username))]
    (go
      (let [{:keys [status body]} (a/<! c)]
        (condp = status
          200 {:for-username username :user-id (:user-id body)}
          404 {:for-username username :user-id :not-found}
          {:for-username username :user-id :error})))))

(let [debounced (util/debounce-chan search-chan 200)]
  (go-loop [search-query (a/<! debounced)]
    (go
      (let [result (a/<! (<lookup-username search-query))]
       (send-page-event! [:username-result result])))
   (recur (a/<! debounced))))

(defn username-updated [username]
  (swap! c/!db assoc-in [:page-state :username-match] :loading)
  (swap! c/!db assoc-in [:page-state :username-query] username)
  (let [c (a/chan)]
    (when (not= username "")
      (a/put! search-chan username))))

(def initial-page-state
  {:username-query ""
   :username-match nil
   :user nil})

(defn username-result-handler [current-username-query {:keys [for-username user-id]}]
  (when (= current-username-query for-username)
    (swap! c/!db assoc-in [:page-state :username-match] user-id)
    (when (not= user-id :not-found)
      (send-page-event! [:fetch-user user-id]))))

(defn fetch-user [user-id]
  (swap! c/!db assoc-in [:page-state :user] [:fetching user-id])
  (let [c (http/get (str "https://api.socialsuperstore.com/v1/users/" user-id))]
    (go
      (let [{:keys [status body]} (a/<! c)]
        (condp = status
          200 (send-page-event! [:user-fetched {:success? true :user body}])
          (send-page-event! [:user-fetched {:success? false}]))))))

(defn user-fetched [{:keys [success? user]}]
  (if success?
    (swap! c/!db assoc-in [:page-state :user] user)
    (swap! c/!db assoc-in [:page-state :user] :fetch-failed)))

(defn page-handler [e app-state]
  (let [page-state (:page-state app-state)
        action
        (match e
          [:navigate :user-page] #(swap! c/!db assoc :page-state initial-page-state)

          [:user-page page-e]
          (match page-e
            [:username-input username] #(username-updated username)
            [:username-result r] #(username-result-handler (:username-query page-state) r)
            [:fetch-user user-id] #(fetch-user user-id)
            [:user-fetched result] #(user-fetched result)
            [:user-pod-follow u] #(c/send-event! [:user-followed u])
            )

          _ identity)]

    (action)))

(defn user-page [app-state]
  (let [user-query (get-in app-state [:page-state :username-query])
        username-match (get-in app-state [:page-state :username-match])]
    [:div.user-search-page
     [:form.user-search
      [:label {:for :username} "Search for a user:"]
      [:input.username {:on-change (fn [e]
                                     (let [q (-> e .-target .-value)]
                                       (send-page-event! [:username-input q])))
                        :value user-query}]

      (when (not= user-query "")
        [:div
         (condp = username-match
           :not-found "No match."
           :loading "Searching.."
           :error "Error"
           "✅")])]

     (let [search-user (get-in app-state [:page-state :user])]
       (when search-user
         [:div.results
          (match search-user
            [:fetching _] [:div "Fetching..."]
            u [sg/user-pod search-user send-page-event!])]))]))

(c/register-handler! :user-page page-handler)
