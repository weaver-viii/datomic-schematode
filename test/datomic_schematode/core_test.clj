(ns datomic-schematode.core-test
  (:require [clojure.test :refer :all]
            [datomic.api :as d]
            [datomic-schematode.core :as ds-core]
            [datomic-schematode.constraints :as ds-constraints]
            [datomic-schematode.core-test.config :as config]))

(use-fixtures :once config/with-db)

(def test-schemas
  [[:user {:attrs [[:username :string :db.unique/identity]
                   [:pwd :string "Hashed password string"]
                   [:email :string :indexed]
                   [:dob :string :indexed]
                   [:lastname :string :indexed]
                   [:status :enum [:pending :active :inactive :cancelled]]
                   [:group :ref :many]]
           :part :app
           :dbfns [(ds-constraints/unique :user :lastname :dob)]}]
   [:group {:attrs [[:name :string]
                    [:permission :string :many]]
            ;; testing without :part
            }]])

(deftest expand-fields-test
  (testing "expand-fields"
    (is (= (ds-core/expand-fields
            (get-in (apply hash-map (flatten test-schemas))
                    [:user :attrs]))
           {"group" [:ref #{:many}], "status" [:enum #{[:pending :active :inactive :cancelled]}], "lastname" [:string #{:indexed}], "dob" [:string #{:indexed}], "email" [:string #{:indexed}], "pwd" [:string #{"Hashed password string"}], "username" [:string #{:db.unique/identity}]}))))

(deftest expand-schemas-test
  (testing "expand-schemas"
    (is (= (ds-core/expand-schemas test-schemas)
           [{:part :db.part/app, :namespace "user", :name "user", :basetype :user, :fields {"group" [:ref #{:many}], "status" [:enum #{[:pending :active :inactive :cancelled]}], "lastname" [:string #{:indexed}], "dob" [:string #{:indexed}], "email" [:string #{:indexed}], "pwd" [:string #{"Hashed password string"}], "username" [:string #{:db.unique/identity}]}} {:part :db.part/user, :namespace "group", :name "group", :basetype :group, :fields {"permission" [:string #{:many}], "name" [:string #{}]}}]))))

(deftest schematize-test
  (testing "schematize"
    (is (= (ds-core/schematize test-schemas (constantly -1))
           '([{:db/id -1, :db/ident :db.part/app, :db.install/_partition :db.part/db}] ({:db/noHistory false, :db/cardinality :db.cardinality/many, :db.install/_attribute :db.part/db, :db/index false, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/ref, :db/ident :user/group, :db/id -1} {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index false, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/ref, :db/ident :user/status, :db/id -1} [:db/add -1 :db/ident :user.status/pending] [:db/add -1 :db/ident :user.status/active] [:db/add -1 :db/ident :user.status/inactive] [:db/add -1 :db/ident :user.status/cancelled] {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index true, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :user/lastname, :db/id -1} {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index true, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :user/dob, :db/id -1} {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index true, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :user/email, :db/id -1} {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index false, :db/fulltext false, :db/doc "Hashed password string", :db/isComponent false, :db/valueType :db.type/string, :db/ident :user/pwd, :db/id -1} {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index false, :db/unique :db.unique/identity, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :user/username, :db/id -1}) ({:db/noHistory false, :db/cardinality :db.cardinality/many, :db.install/_attribute :db.part/db, :db/index false, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :group/permission, :db/id -1} {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index false, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :group/name, :db/id -1}))))))

(deftest load-schema!-test
  (testing "load-schema!"
    (is (= (map #(keys (deref %))
                (ds-core/load-schema! (d/connect config/db-url) [[:u {:attrs [[:a :string]]}]]))
           '((:db-before :db-after :tx-data :tempids) (:db-before :db-after :tx-data :tempids))))))
