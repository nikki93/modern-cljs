(ns modern-cljs.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))


(enable-console-print!)


(def init-data
  {:list/one [{:name "John" :points 0}
              {:name "Mary" :points 0}
              {:name "Bob" :points 0}]
   :list/two [{:name "Mary" :points 0 :age 27}
              {:name "Gwen" :points 0}
              {:name "Jeff" :points 0}]})


(defmulti read om/dispatch)

(defn get-people [state key]
  (let [st @state]
    (into [] (map #(get-in st %)) (get st key))))

(defmethod read :list/one
  [{:keys [state] :as env} key params]
  {:value (get-people state key)})

(defmethod read :list/two
  [{:keys [state] :as env} key params]
  {:value (get-people state key)})


(defmulti mutate om/dispatch)

(defmethod mutate 'points/increment
  [{:keys [state]} _ {:keys [name]}]
  {:action
   (fn []
     (swap! state update-in [:person/by-name name :points] inc))})

(defmethod mutate 'points/decrement
  [{:keys [state]} _ {:keys [name]}]
  {:action
   (fn []
     (swap! state update-in [:person/by-name name :points]
            (comp (partial max 0) dec)))})


(defui Person
  static om/Ident
  (ident [this {:keys [name]}]
         [:person/by-name name])
  static om/IQuery
  (query [this]
         '[:name :points])
  Object
  (render [this]
          (let [{:keys [name points] :as props} (om/props this)]
            (println "rendering person" name)
            (dom/li nil
                    (dom/label nil (str name ", points: " points))
                    (dom/button
                     #js {:onClick #(om/transact! this `[(points/increment ~props)])}
                     "+")
                    (dom/button
                     #js {:onClick #(om/transact! this `[(points/decrement ~props)])}
                     "-")))))

(def person (om/factory Person {:keyfn :name}))


(defui ListView
  Object
  (render [this]
          (println "rendering list-view" (first (om/path this)))
          (apply dom/ul nil (map person (om/props this)))))

(def list-view (om/factory ListView))


(defui RootView
  static om/IQuery
  (query [this]
         (let [subquery (om/get-query Person)]
           `[{:list/one ~subquery} {:list/two ~subquery}]))
  Object
  (render [this]
          (println "rendering root-view")
          (let [{:keys [list/one list/two]} (om/props this)]
            (dom/div nil
                     (dom/h2 nil "list one")
                     (list-view one)
                     (dom/h2 nil "list two")
                     (list-view two)))))


(def reconciler
  (om/reconciler {:state init-data
                  :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler RootView (gdom/getElement "app"))


(comment

  (require '[cljs.pprint :as pp])

  (def norm-data (om/tree->db RootView init-data true))

  (pp/pprint norm-data)

  (def parser (om/parser {:read read :mutate mutate}))

  (def st (atom norm-data))

  (parser {:state st} '[(points/increment {:name "Mary"})])

  (parser {:state st} '[:list/one])

  (parser {:state st} '[:list/two])

  (om/transact! reconciler '[(points/increment {:name "Mary"})])

  )
