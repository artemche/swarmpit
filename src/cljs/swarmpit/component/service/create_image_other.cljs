(ns swarmpit.component.service.create-image-other
  (:require [material.component :as comp]
            [swarmpit.component.state :as state]
            [swarmpit.storage :as storage]
            [swarmpit.url :refer [dispatch!]]
            [clojure.walk :refer [keywordize-keys]]
            [swarmpit.routes :as routes]
            [rum.core :as rum]
            [ajax.core :as ajax]))

(def cursor [:page :service :wizard :image :other])

(defn- render-item
  [item]
  (let [value (val item)]
    value))

(defn- repository-handler
  [registry query]
  (ajax/GET (routes/path-for-backend :repositories {:registryName registry})
            {:headers {"Authorization" (storage/get "token")}
             :params  {:repositoryQuery query}
             :finally (state/update-value [:searching] true cursor)
             :handler (fn [response]
                        (let [res (keywordize-keys response)]
                          (state/update-value [:searching] false cursor)
                          (state/update-value [:data] res cursor)))}))

(defn- form-registry [registry registries]
  (comp/form-comp
    "REGISTRY"
    (comp/select-field
      {:value    registry
       :onChange (fn [_ _ v]
                   (state/update-value [:data] [] cursor)
                   (state/update-value [:registry] v cursor))}
      (->> registries
           (map #(comp/menu-item
                   {:key         %
                    :value       %
                    :primaryText %}))))))

(defn- form-repository [registry repository]
  (comp/form-comp
    "REPOSITORY"
    (comp/text-field
      {:hintText "Find repository"
       :value    repository
       :onChange (fn [_ v]
                   (state/update-value [:repository] v cursor)
                   (repository-handler registry v))})))

(rum/defc form-loading < rum/static []
  (comp/form-comp-loading true))

(rum/defc form-loaded < rum/static []
  (comp/form-comp-loading false))

(defn- repository-list [registry data]
  (let [repository (fn [index] (:name (nth data index)))]
    (comp/mui
      (comp/table
        {:key         "tbl"
         :selectable  false
         :onCellClick (fn [i]
                        (dispatch!
                          (routes/path-for-frontend :service-create-config
                                                    {}
                                                    {:repository (repository i)
                                                     :registry   registry})))}
        (comp/list-table-header ["Name"])
        (comp/list-table-body data
                              render-item
                              [[:name]])))))

(rum/defc form < rum/reactive [registries]
  (let [{:keys [searching
                repository
                registry
                data]} (state/react cursor)]
    (if (some? registry)
      [:div.form-edit
       (form-registry registry registries)
       (form-repository registry repository)
       [:div.form-edit-loader
        (if searching
          (form-loading)
          (form-loaded))
        (repository-list registry data)]]
      [:div "No custom registries found. Please ask your admin to create some :)"])))