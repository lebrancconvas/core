(ns dev.components
  (:require [core.component :as component]))

 ; https://gist.github.com/pierrejoubert73/902cc94d79424356a8d20be2b382e1ab
 ; https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/organizing-information-with-collapsed-sections
 ; -> and after each 'build' I can have a bash script which uploads the components go github

(comment
 (print-txs "txs.md")
 (print-components "components.md")
 )

(defn print-txs [file]
  (spit file
        (binding [*print-level* nil]
          (with-out-str
           (doseq [[nmsp ks] (sort-by first
                                      (group-by namespace (sort (keys (methods component/do!)))))]

             (println "\n#" nmsp)
             (doseq [k ks
                     :let [attr-m (get component/attributes k)]]
               (println (str "* __" k "__ `" (get (:params attr-m) "do!") "`"))
               (when-let [data (:data attr-m)]
                 (println (str "    * data: `" (pr-str data) "`")))
               (let [ks (descendants k)]
                 (when (seq ks)
                   (println "    * Descendants"))
                 (doseq [k ks]
                   (println "      *" k)
                   (println (str "        * data: `" (pr-str (:data (get component/attributes k))) "`"))))))))))

(defn- component-systems [component-k]
   (for [[sys-name sys-var] component/defsystems
         [k method] (methods @sys-var)
         :when (= k component-k)]
     sys-name))

(defn print-components [file]
  (spit file
        (binding [*print-level* nil]
          (with-out-str
           (doseq [[nmsp components] (sort-by first
                                              (group-by namespace
                                                        (sort (keys component/attributes))))]
             (println "\n#" nmsp)
             (doseq [k components]
               (println "*" k
                        (if-let [ancestrs (ancestors k)]
                          (str "-> "(clojure.string/join "," ancestrs))
                          "")
                        (let [attr-map (get component/attributes k)]
                          (if (seq attr-map)
                            (pr-str (:core.component/fn-params attr-map))
                            #_(binding [*print-level* nil]
                                (with-out-str
                                 (clojure.pprint/pprint attr-map)))
                            "")))
               #_(doseq [system-name (component-systems k)]
                   (println "  * " system-name))))))))
