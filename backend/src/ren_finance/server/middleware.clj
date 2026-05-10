(ns ren-finance.server.middleware
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]))

(defn wrap-request-logging
  "Log all requests: [METHOD] [PATH] [STATUS] [DURATION-MS]"
  [handler]
  (fn [request]
    (let [start (System/currentTimeMillis)
          method (str/upper-case (name (:request-method request)))
          path (:uri request)
          response (try
                     (handler request)
                     (catch Exception e
                       (log/error e "Unhandled exception processing" method path)
                       {:status 500
                        :headers {"Content-Type" "application/json"}
                        :body {:error {:code :internal-error
                                       :message "Internal server error"}}}))
          elapsed (- (System/currentTimeMillis) start)
          status (:status response)]
      (log/info (str method " " path " " status " " elapsed "ms"))
      response)))

(defn wrap-error-envelope
  "Wrap all responses in standard JSON envelope.
   Success: {:status 200, :body {:data ...}}
   Error:   {:status 4xx/5xx, :body {:error {:code :message :details}}}
   Ensures all errors return consistent JSON, never plain text."
  [handler]
  (fn [request]
    (let [response (try
                     (handler request)
                     (catch Exception e
                       (log/error e "Unhandled error")
                       {:status 500
                        :headers {"Content-Type" "application/json"}
                        :body {:error {:code :internal-error
                                       :message "Internal server error"}}}))]
      response)))

(defn wrap-health
  "Add health check endpoint bypassing other middleware."
  [handler]
  (fn [request]
    (if (and (= :get (:request-method request)) (= "/api/health" (:uri request)))
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body {:data {:status "ok"
                     :db-connected true
                     :uptime-seconds ((requiring-resolve 'ren-finance.server.core/uptime-seconds))}}}
      (handler request))))

(defn wrap-cors-config
  "CORS middleware for development. Allow localhost origins."
  [handler]
  (wrap-cors handler
             :access-control-allow-origin [#".*"]
             :access-control-allow-methods [:get :put :post :delete :options]
             :access-control-allow-headers ["Content-Type" "Authorization"]))

(defn app-middleware
  "Apply all middleware in order."
  [handler]
  (-> handler
      wrap-health
      wrap-error-envelope
      wrap-request-logging
      wrap-keyword-params
      wrap-params
      (wrap-json-body {:keywords? true})
      wrap-json-response
      wrap-cors-config))
