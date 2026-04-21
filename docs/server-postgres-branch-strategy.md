# Server PostgreSQL統一ブランチ戦略

## 方針
- 本戦略は「サーバーPostgreSQLを正本」にするための段階的統合を目的とする。
- 小さなPRで機能ごとにリスク分割する。

## ブランチ命名
- `feat/server-db-unify-01-move-api`
- `feat/server-db-unify-02-stock-query-api`
- `feat/server-db-unify-03-stock-history-api`
- `feat/server-db-unify-04-stocktake-query-api`
- `feat/server-db-unify-05-cancel-api`
- `chore/server-db-unify-06-room-shrink`

## PR順序（依存順）
1. Move API追加（server-ktor + connector-db）
2. searchStockのサーバー化
3. getStockHistoryのサーバー化
4. getStocktakeSummaries/getStocktakeDetailsのサーバー化
5. cancelOperationのサーバー化
6. Room依存の縮小

## 完了条件
- HybridInventoryGatewayで業務系メソッドがサーバー経路に統一される。
- 画面の在庫/履歴/棚卸データがサーバー由来になる。
- Roomは必要最小限（または廃止）に整理される。
