(ns ren-finance.mcp.tools)

(def all-tools
  "List of tool definitions. Each tool is {:name :title :description :schema :tool-fn}.
   Tools are implemented in Task 10. Here they return placeholder responses.
   
   tool-fn signature: (fn [req args] ...) where args are the tool's input params."
  [{:name "get_net_worth"
    :title "Get Net Worth"
    :description "Get current net worth breakdown including account types and last sync time."
    :schema {}
    :tool-fn (fn [req args]
               {:content [{:type "text"
                           :text "Net worth query not yet implemented. Use Task 10 to enable."}]
                :isError false})}
   {:name "fetch_wallet_data"
    :title "Fetch Wallet Data"
    :description "Trigger wallet address sync to refresh balances."
    :schema {:addresses {:type "array"
                         :items {:type "string"}
                         :description "Wallet addresses to sync"}}
    :tool-fn (fn [req args]
               {:content [{:type "text"
                           :text "Wallet sync not yet implemented. Use Task 10 to enable."}]
                :isError false})}
   {:name "list_transactions"
    :title "List Transactions"
    :description "List recent transactions with optional limit and category filter."
    :schema {:limit {:type "number" :description "Max transactions to return"}
             :category {:type "string" :description "Filter by category"}}
    :tool-fn (fn [req args]
               {:content [{:type "text"
                           :text "Transaction list not yet implemented. Use Task 10 to enable."}]
                :isError false})}
   {:name "generate_report"
    :title "Generate Report"
    :description "Generate spending/income report for a date range."
    :schema {:start-date {:type "string" :description "Start date (YYYY-MM-DD)"}
             :end-date {:type "string" :description "End date (YYYY-MM-DD)"}}
    :tool-fn (fn [req args]
               {:content [{:type "text"
                           :text "Report generation not yet implemented. Use Task 10 to enable."}]
                :isError false})}])
