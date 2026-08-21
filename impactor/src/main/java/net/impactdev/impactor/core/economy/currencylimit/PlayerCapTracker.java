/*
 * This file is part of Impactor, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018-2022 NickImpact
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package net.impactdev.impactor.core.economy.currencylimit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCapTracker {

    private static final File PLAYERS_DIR = new File("config/HarmonyLib/currencylimit/players");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<UUID, Map<String, BigDecimal>> cache = new ConcurrentHashMap<>();

    public static void init() {
        PLAYERS_DIR.mkdirs();
    }

    private static Map<String, BigDecimal> getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> {
            File file = new File(PLAYERS_DIR, u.toString() + ".json");
            if (!file.exists()) return new HashMap<>();
            try (FileReader reader = new FileReader(file)) {
                Map<String, BigDecimal> data = GSON.fromJson(reader, Map.class);
                return data != null ? new HashMap<>(mapToBigDecimal(data)) : new HashMap<>();
            } catch (IOException e) {
                System.out.println("[Impactor] Failed to load cap tracker for " + u);
                return new HashMap<>();
            }
        });
    }

    private static Map<String, BigDecimal> mapToBigDecimal(Map<String, ?> raw) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : raw.entrySet()) {
            result.put(entry.getKey(), new BigDecimal(entry.getValue().toString()));
        }
        return result;
    }

    private static void save(UUID uuid, Map<String, BigDecimal> data) {
        File file = new File(PLAYERS_DIR, uuid.toString() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.out.println("[Impactor] Failed to save cap tracker for " + uuid);
        }
    }


    public static BigDecimal getGained(UUID uuid, String currencyKey) {
        return getOrLoad(uuid).getOrDefault(currencyKey.toLowerCase(), BigDecimal.ZERO);
    }


    public static void addGained(UUID uuid, String currencyKey, BigDecimal amount) {
        Map<String, BigDecimal> data = getOrLoad(uuid);
        String key = currencyKey.toLowerCase();
        data.put(key, data.getOrDefault(key, BigDecimal.ZERO).add(amount));
        save(uuid, data);
    }


    public static int resetAll(String currencyKey) {
        String key = currencyKey.toLowerCase();
        File[] files = PLAYERS_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return 0;

        int count = 0;
        for (File file : files) {
            String uuidStr = file.getName().replace(".json", "");
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            Map<String, BigDecimal> data = getOrLoad(uuid);
            if (data.containsKey(key)) {
                data.put(key, BigDecimal.ZERO);
                save(uuid, data);
                cache.put(uuid, data);
                count++;
            }
        }
        return count;
    }


    public static int resetAllCurrencies(java.util.Set<String> currencyKeys) {
        File[] files = PLAYERS_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return 0;

        int count = 0;
        for (File file : files) {
            String uuidStr = file.getName().replace(".json", "");
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            Map<String, BigDecimal> data = getOrLoad(uuid);
            boolean changed = false;
            for (String key : currencyKeys) {
                String k = key.toLowerCase();
                if (data.containsKey(k)) {
                    data.put(k, BigDecimal.ZERO);
                    changed = true;
                }
            }
            if (changed) {
                save(uuid, data);
                cache.put(uuid, data);
                count++;
            }
        }
        return count;
    }
}