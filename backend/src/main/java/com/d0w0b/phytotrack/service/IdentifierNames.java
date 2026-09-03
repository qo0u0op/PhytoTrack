package com.d0w0b.phytotrack.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 簽名人名稱正規化（IdentifierNames）
 *
 * 所有簽名人重名比對的唯一真相源：
 * trim、連續空白摺疊、全半形統一 (NFKC)、Unicode 正規化 (NFC)、英文大小寫不敏感。
 */
public final class IdentifierNames {

  private IdentifierNames () {
  }

  public static String normalize (String raw) {
    if (raw == null) {
      return "";
    }
    String t = Normalizer.normalize (raw, Normalizer.Form.NFKC).trim ().replaceAll ("\\s+", " ");
    t = Normalizer.normalize (t, Normalizer.Form.NFC);
    return t.toLowerCase (Locale.ROOT);
  }

  public static boolean equalsNormalized (String a, String b) {
    if (a == null || b == null) {
      return false;
    }
    return normalize (a).equals (normalize (b));
  }

  public static String display (String raw) {
    if (raw == null) {
      return "";
    }
    return Normalizer.normalize (raw, Normalizer.Form.NFKC).trim ().replaceAll ("\\s+", " ");
  }
}
