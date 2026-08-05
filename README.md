おことわり コードの大半をChatGPTが書いていますが、問題はないと思います。

# ManualMinecart

Spigot 1.21.1とTrainCarts用の手動運転プラグインです。

## 導入

1. `ManualMinecart-1.0.0.jar`、`BKCommonLib`、`Train_Carts`をサーバーへ導入します。
2. サーバーを再起動します。

## 使い方

1. TrainCartsの列車に乗り、`/mmc set`を実行します。
2. `/mmc stick`で運転用の棒を受け取ります。
3. 棒を持ったまま右クリックすると力行側、左クリックすると制動側へ1段動きます。
4. 停止した状態で反対を向き力行すると方向転換ができます。

ノッチ順は`EB - B3 - B2 - B1 - N - P1 - P2 - P3`です。最高速度は５０ｋｍ／ｈです。

連結列車でも動作しますが、乗車して運転するトロッコにはすべて`/mmc set`の実行が必要です。

## ビルド

JDK 21とMavenを使い、プロジェクトのルートで次を実行します。

```bash
mvn clean package
```

完成したJARは`target/ManualMinecart-1.0.0.jar`です。
