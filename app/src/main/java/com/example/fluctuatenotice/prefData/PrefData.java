package com.example.fluctuatenotice.prefData;

public class PrefData {

    public static final String[] pref_name = {
            "宗谷地方", "上川・留萌地方", "石狩・空知・後志地方", "網走・北見・紋別地方",
            "十勝・釧路・根室地方", "胆振・日高地方", "渡島・檜山地方",
            "青森県", "秋田県", "岩手県", "宮城県", "山形県", "福島県",
            "茨城県", "栃木県", "群馬県", "埼玉県", "東京都", "千葉県", "神奈川県",
            "長野県", "山梨県", "静岡県", "愛知県", "岐阜県", "三重県", "新潟県", "富山県", "石川県", "福井県",
            "滋賀県", "京都府", "大阪府", "兵庫県", "奈良県", "和歌山県",
            "岡山県", "広島県", "島根県", "鳥取県", "徳島県", "香川県", "愛媛県", "高知県",
            "山口県", "福岡県", "大分県", "長崎県", "佐賀県", "熊本県", "宮崎県", "鹿児島県", "鹿児島県（奄美地方）",
            "沖縄県（本島地方）", "沖縄県（大東島地方）", "沖縄県（宮古島地方）", "沖縄県（八重山地方）"
    };

    public static final String[] pref_code = {
            "11", "12", "14", "17", "19", "21", "23", "31", "32", "33", "34", "35", "36", "40", "41",
            "42", "43", "44", "45", "46", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57",
            "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "71", "72", "73", "74", "81",
            "82", "83", "84", "85", "86", "87", "88", "88", "91", "91", "91", "91"
    };

    // 「過去の気象データ」ページ取得用
    // アメダス観測地点名
    public static final String[] station_name = {
            "稚内地方気象台", "旭川地方気象台", "札幌管区気象台", "網走地方気象台",
            "釧路地方気象台", "室蘭地方気象台", "函館地方気象台",
            "青森地方気象台", "秋田地方気象台", "盛岡地方気象台", "山形地方気象台", "仙台管区気象台", "福島地方気象台",
            "水戸地方気象台", "宇都宮地方気象台", "前橋地方気象台", "熊谷地方気象台",
            "気象庁", "銚子地方気象台", "横浜地方気象台",
            "長野地方気象台", "甲府地方気象台", "静岡地方気象台", "名古屋地方気象台", "岐阜地方気象台", "津地方気象台",
            "新潟地方気象台", "富山地方気象台", "金沢地方気象台", "福井地方気象台",
            "彦根地方気象台", "京都地方気象台", "大阪管区気象台", "神戸地方気象台", "奈良地方気象台", "和歌山地方気象台",
            "岡山地方気象台", "広島地方気象台", "松江地方気象台", "鳥取地方気象台",
            "徳島地方気象台", "高松地方気象台", "松山地方気象台", "高知地方気象台",
            "下関地方気象台", "福岡管区気象台", "大分地方気象台", "長崎地方気象台", "佐賀地方気象台", "熊本地方気象台",
            "宮崎地方気象台", "鹿児島地方気象台", "名瀬測候所",
            "沖縄気象台", "南大東島地方気象台", "宮古島地方気象台", "石垣島地方気象台"
    };

    // アメダス観測地点コード
    public static final String[] station_code = {
            "47401", "47407", "47412", "47409", "47418", "47423", "47430", "47575", "47582",
            "47584", "47588", "47590", "47595", "47629", "47615", "47624", "47626", "47662",
            "47648", "47670", "47610", "47638", "47656", "47636", "47632", "47651", "47604",
            "47607", "47605", "47616", "47761", "47759", "47772", "47770", "47780", "47777",
            "47768", "47765", "47741", "47746", "47895", "47891", "47887", "47893", "47762",
            "47807", "47815", "47817", "47813", "47819", "47830", "47827", "47909", "47936",
            "47945", "47927", "47918"
    };

    // 苦肉の策
    public static float[] temperatures = {0.0f, 0.0f, 0.0f, 0.0f};
    public static String[] URLs = {"", ""};

}
