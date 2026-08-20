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
import java.util.Set;

public class CurrencyLimitConfig {

    private static final File FILE = new File("config/HarmonyLib/currencylimit/config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, BigDecimal> caps = new HashMap<>();

    public static void load() {
        if (!FILE.exists()) {
            caps = new HashMap<>();
            save();
            System.out.println("[Impactor] currencylimit/config.json not found, created empty.");
            return;
        }
        try (FileReader reader = new FileReader(FILE)) {
            Data data = GSON.fromJson(reader, Data.class);
            caps = (data != null && data.caps != null) ? data.caps : new HashMap<>();
        } catch (IOException e) {
            System.out.println("[Impactor] Failed to load currencylimit/config.json, using empty caps.");
            caps = new HashMap<>();
        }
    }

    private static void save() {
        FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(FILE)) {
            Data data = new Data();
            data.caps = caps;
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.out.println("[Impactor] Failed to save currencylimit/config.json.");
        }
    }

    public static BigDecimal getCap(String currencyKey) {
        return caps.getOrDefault(currencyKey.toLowerCase(), new BigDecimal(Long.MAX_VALUE));
    }

    public static void setCap(String currencyKey, BigDecimal cap) {
        caps.put(currencyKey.toLowerCase(), cap);
        save();
    }


    public static Set<String> cappedCurrencyKeys() {
        return caps.keySet();
    }

    private static class Data {
        Map<String, BigDecimal> caps;
    }
}