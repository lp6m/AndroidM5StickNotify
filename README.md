# M5Stick + Android Notification App

<img src=./image/image.jpg width=60%>  

## セットアップ
### M5Stick/M5StickCPlus
VSCode + PlatformIOをインストールし、`m5stick/tikatika`ディレクトリをVSCodeで開きます。  
VSCodeのコマンドパレットから`PlatformIO: Build`を選択しビルドした後、`PlatformIO: Upload`でスケッチをM5Stickに書き込みます。M5StickCPlusで開発していますが、M5Stickを使用する場合は適宜ライブラリを選択しなおしてください。

### Android
`android/app/release/app-release.apk`をAndroidデバイスに転送し、インストールします。  

## 使い方
### ペアリング
M5Stickを起動すると、自動的にペアリング待機状態になります。  
Android本体設定からBluetoothデバイスを探し、ペアリングを行なってください。  
アプリを起動します。画面左下部の`App Setting`から通知を送りたいアプリを選択します。選択したアプリ情報は保存されます。  
画面右下部の`Bluetooth`からBluetoothメニューを開き、`SELECT DEVICE AND CONNECT`ボタンを押すとデバイスがペアリングしたことのあるデバイス一覧が表示されます。`M5Stack`を探し選択してください。選択すると自動的にM5StickとBLE接続を行います。接続に成功すると、アプリ画面下部に`Device M5Stack Connected`と表示されます。M5Stick側には`connected`の文字が表示されます。

次回以降はアプリ本体にM5Stickのデバイス情報が保存されるため、起動時にM5Stickがペアリング待受状態であれば自動的に接続します。

### 通知時動作・通知内容設定
画面右下部の`Bluetooth`からBluetoothメニューで以下の設定が行えます。  
- LED only: 選択した場合、次回通知からM5StickのLCDは消灯し、LEDのみが点灯します。
- Send Icon: 選択した場合、通知内容とともに通知アイコンを転送します。  
通知アイコンの転送は転送量が多いため、メッセージよりも遅延します(1〜2秒程度)  

Android側で通知を削除すると、M5StickのLCDに表示された通知が削除されます。  
ただし、M5Stick側では過去の通知情報を保存していないため、M5Stickに表示されていない通知を削除した場合でも通知が削除されます。  
通知を転送するとして選択していないアプリの通知を削除した場合は、特に何も行われません。


### 通知取得サービスについて
通知取得・デバイスへの通知送信部分はサービスとして動作しているため、アプリ自体を終了しても動作し続けます。完全に停止したい場合は「アプリを停止」を行なってください。これによりアプリに関連づいているサービスごと終了します。

### 再起動・再接続について
サービス起動中に何らかの原因によりBLE接続が失われた際、再度通信可能状態になればM5Stickとアプリは自動再接続します。再接続にどうしても失敗する場合は、以下のリセットが可能です。
- M5Stick側: BボタンによりM5Stickごと再起動
- アプリ側: Bluetoothメニューの`RECONNECT`ボタンによりサービス再起動


## 実装について

### BLE Service
画像転送用、通知テキスト転送用に2種類のBLE Serviceを実装しています。それぞれのサービスに以下のUUIDを使用しています。
```cpp
#define CHARACTERISTIC_IMAGE_UUID "ba9aecfc-d4c7-4688-b67d-bf76ab618105"
#define CHARACTERISTIC_TEXT_UUID "beb5483f-36e1-4688-b7f5-ea07361b26a8"
```

LEDのみの通知、通知削除にはBLE Serviceを使用せず、通知テキスト転送のサービスをそのまま使用しています。
```cpp
#define LED_ONLY_COMMAND "LED_ONLY_COMMAND"
#define CLEAR_COMMAND "CLEAR_COMMAND"
```
通知テキストの内容が上記の定数値と一致した場合は、通知ではなくコマンドであるとしてM5Stick側が動作するようになっています。  

### Android App

- `MainActivity.java`: 2つのフラグメントを格納するメインのActivity
- `NotificationDbHelper.java`: 通知するアプリ一覧の情報を保存するために必要なDBヘルパー
- `NotificationReporterService.java`: `NotificationListenerService`を継承したサービスとして常駐し、通知をトリガーとしてBLE転送を行うクラス
- `BluetoothLEWork.java`: BLE通信を行うためのクラスで`NotificationReporterService`によりインスタンスとして保持される
- `ui/home/HomeFragment`: 通知するアプリ一覧を表示するGUI Fragment
- `ui/dashboard/DashboardFragment`: Bluetooth接続・設定を表示するGUI Fragment





