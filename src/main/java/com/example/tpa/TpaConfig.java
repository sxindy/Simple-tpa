package com.example.tpa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class TpaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("simpletpa.json");
    private static TpaConfig instance = new TpaConfig();

    /** แสดงหน้า UI ยืนยันให้ผู้ส่งก่อนส่งคำขอ */
    public boolean showConfirmBeforeSending = true;
    /** เปิดหน้า UI ให้ผู้รับอัตโนมัติเมื่อได้รับคำขอ (ถ้า false จะมีแค่ข้อความในแชท) */
    public boolean openGuiForTarget = true;
    /** คำขอหมดอายุภายในกี่วินาที */
    public int requestTimeoutSeconds = 60;
    /** คูลดาวน์ระหว่างการส่งคำขอ (วินาที) */
    public int cooldownSeconds = 3;
    /** อนุญาตให้ tp ข้ามมิติ */
    public boolean allowCrossDimension = true;

    public static TpaConfig get() { return instance; }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                TpaConfig c = GSON.fromJson(Files.readString(PATH), TpaConfig.class);
                if (c != null) instance = c;
            }
            Files.writeString(PATH, GSON.toJson(instance));
        } catch (Exception e) {
            TpaMod.LOGGER.error("Simple TPA: config error, using defaults", e);
        }
    }
}
