(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.repl :refer :all]
            [fipp.edn :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [duct.core :as duct]
            [duct.core.repl :as duct-repl :refer [auto-reset]]
            [eftest.runner :as eftest]
            [integrant.core :as ig]
            [integrant.repl :refer [clear halt go init prep]]
            [integrant.repl.state :refer [config system]]
            [ragtime.jdbc]
            [ragtime.repl]
            [orchestra.spec.test :as stest]
            [todo-api.main :refer [custom-readers]]))

(duct/load-hierarchy)

(defn read-config []
  (duct/read-config (io/resource "todo_api/config.edn")
                    custom-readers))

(defn reset []
  (let [result (integrant.repl/reset)]
    (with-out-str)))

(defn test []
  (eftest/run-tests (eftest/find-tests "test")))

(def env-profiles
  {"dev" [:duct.profile/dev :duct.profile/local]})

(defn- validate-env [env]
  (when-not (some #{env} (keys env-profiles))
    (throw (IllegalArgumentException. (format "env `%s` is undefined" env)))))

(defn- load-migration-config [env]
  (when-let [profiles (get env-profiles env)]
    (let [prepped (duct/prep-config (read-config) profiles)
          {{:keys [connection-uri]} :duct.database.sql/hikaricp} prepped
          resources-key :duct.migrator.ragtime/resources]
      {:datastore (ragtime.jdbc/sql-database connection-uri)
       :migrations (-> prepped
                      (ig/init [resources-key])
                      (get resources-key))})))

(defn db-migrate [env]
  (validate-env env)
  (ragtime.repl/migrate (load-migration-config env)))

(defn db-rollback [env]
  (validate-env env)
  (ragtime.repl/rollback (load-migration-config env)))

(def profiles
  [:duct.profile/dev :duct.profile/local])

(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")

(when (io/resource "local.clj")
  (load "local"))

(integrant.repl/set-prep! #(duct/prep-config (read-config) profiles))
