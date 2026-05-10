(ns ren-finance.mcp.server
  (:require [co.gaiwan.mcp :as mcp]
            [co.gaiwan.mcp.state :as mcp-state]
            [co.gaiwan.mcp.system.http :as http]
            [ren-finance.mcp.tools :as tools]
            [ren-finance.mcp.resources :as resources]
            [clojure.tools.logging :as log]))

(defn- register-tools!
  "Register all MCP tools in global state atom."
  []
  (doseq [{:keys [name title description schema tool-fn]} tools/all-tools]
    (mcp-state/add-tool
     {:name name
      :title title
      :description description
      :schema schema
      :tool-fn tool-fn})))

(defn- register-resources!
  "Register all MCP resources in global state atom."
  []
  (doseq [{:keys [uri name title description mime-type load-fn]} resources/all-resources]
    (mcp-state/add-resource
     {:uri uri
      :name name
      :title title
      :description description
      :mime-type mime-type
      :load-fn load-fn})))

(defn create-server
  "Initialize MCP server state with ren-finance info and capabilities.
   Registers tools and resources in the global state atom.
   Returns the opts map for use with start-mcp!."
  []
  (swap! mcp-state/state assoc
         :server-info {:name "ren-finance"
                       :title "Ren Finance MCP Server"
                       :version "0.1.0"}
         :capabilities {:logging {}
                        :tools {:listChanged true}
                        :resources {:listChanged true}}
         :instructions "")
  (register-tools!)
  (register-resources!)
  (log/info "MCP server initialized with" (count tools/all-tools) "tools and"
            (count resources/all-resources) "resources"))

(defn start-mcp!
  "Start MCP server with configured transport.
   Transport from config: :stdio (default) or :http.
   Reads REN_MCP_TRANSPORT and REN_MCP_HTTP_PORT env vars."
  [& {:keys [transport http-port]
      :or {transport (keyword (or (System/getenv "REN_MCP_TRANSPORT") "stdio"))
           http-port (or (some-> (System/getenv "REN_MCP_HTTP_PORT") Integer/parseInt) 3999)}}]
  (create-server)
  (log/info "Starting MCP server with transport:" transport)
  (if (= transport :http)
    (do
      (mcp/run-http! {:port http-port})
      (log/info "MCP HTTP server started on port" http-port))
    (do
      (mcp/run-stdio! nil)
      (log/info "MCP STDIO server started"))))

(defn stop-mcp!
  "Stop MCP HTTP server. Resets global state."
  [jetty]
  (when jetty
    (log/info "Stopping MCP HTTP server...")
    (http/stop! jetty)
    (reset! mcp-state/state (mcp-state/initial-state))
    (log/info "MCP server stopped.")))
