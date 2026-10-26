# Simple TPA (Fabric 1.21.1, server-side)

Build: JDK 21 + internet
    gradle wrapper --gradle-version 8.10
    ./gradlew build
-> build/libs/simple-tpa-1.0.0.jar  (วางใน mods ของเซิร์ฟเวอร์ + Fabric API) ผู้เล่นไม่ต้องลงมอด

Commands (ทุกคนใช้ได้)
  /tpa <player>        ขอเทเลพอร์ตไปหาผู้เล่นนั้น
  /tpahere <player>    ขอให้ผู้เล่นนั้นเทเลพอร์ตมาหาเรา
  /tpaccept [player]   ยอมรับคำขอ
  /tpdeny [player]     ปฏิเสธคำขอ

Config: config/simpletpa.json
