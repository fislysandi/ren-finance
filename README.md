# ren-finance

Personal finance tracking app ("Netwars" dashboard). 
Automatically fetches crypto wallet balances, imports CSV files, 
and exposes data via MCP for AI agent integration.

## Architecture

- **Backend**: Clojure + Datahike (file-based Datalog DB) + Aleph HTTP + Gaiwan MCP SDK
- **Frontend**: ClojureDart (Flutter) + re-dash state management
- **Storage**: File-based Datahike (no database server)
- **Crypto**: Public blockchain API wallet lookup (Etherscan ETH, Blockchain.com BTC)
- **Deployment**: Docker + Nginx + Let's Encrypt HTTPS

## Quick Start

### Backend
```bash
cd backend
clojure -M -m ren-finance.core
```

### Frontend
```bash
cd frontend
clj -M:cljd compile
flutter build web --release
```

### Tests
```bash
cd backend
clojure -M:test
```

## Configuration

See `.env.example` for all config options. Config priority: env vars > config.edn > defaults.

## License

MIT
