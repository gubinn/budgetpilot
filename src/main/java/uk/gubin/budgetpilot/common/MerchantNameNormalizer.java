package uk.gubin.budgetpilot.common;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 商户名称归一化。
 *
 * <p>银行/支付渠道给到的商户名通常是「渠道 + 收单主体 + 门店」的原始字符串，
 * 例如“支付宝-支付宝-消费-北京三快在线科技…快捷支付”。直接入库会为每一笔
 * 交易创建一个商户。这里把原始名称清洗成可聚合的品牌名，原始名称由
 * {@code t_merchant_alias} 保留，供分类学习和回溯使用。</p>
 */
@Component
public class MerchantNameNormalizer {

    /** 支付渠道前缀，按长度从长到短匹配。 */
    private static final List<String> PREFIXES = List.of(
            "支付宝-支付宝-消费-",
            "财付通-微信支付-",
            "银联扣款-银联在线支付，",
            "银联扣款-银联在线支付,",
            "支付宝-消费-",
            "Apple Pay, 线上支付消费，",
            "Apple Pay, 线上支付消费,",
            "支付宝-",
            "财付通-",
            "微信支付-",
            "京东支付-",
            "银联扣款-",
            "银联-",
            "滴滴支付-",
            "抖音支付-",
            "程支付-",
            "Apple Pay",
            "数字人民币随用随充消费"
    );

    /** 渠道本身就能确定品牌的情况（仅在名称里没有更具体品牌时使用）。 */
    private static final Map<String, String> CHANNEL_BRANDS = Map.of(
            "京东支付-", "京东",
            "抖音支付-", "抖音",
            "滴滴支付-", "滴滴出行",
            "程支付-", "携程",
            "数字人民币随用随充消费", "数字人民币"
    );

    /** 交易描述后缀，按长度从长到短匹配。 */
    private static final List<String> SUFFIXES = List.of(
            "扫二维码付款",
            "线上支付消费",
            "快捷支付",
            "扫码付款",
            "消费"
    );

    /** 企业法人类后缀（含邮件截断后的残缺形式）。 */
    private static final List<String> LEGAL_SUFFIXES = List.of(
            "股份有限公司",
            "有限责任公司",
            "个体工商户",
            "股份有限公",
            "有限责任公",
            "有限公司",
            "分公司",
            "有限公",
            "集团",
            "公司",
            "有限"
    );

    private static final Pattern BRANCH_SUFFIX = Pattern.compile("[（(][^（）()]*店[）)]$");

    private static final List<BrandAlias> BRANDS = buildBrands();

    /**
     * 把原始商户名归一化为聚合后的商户名。
     */
    public String canonicalName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "";
        }
        String raw = rawName.trim();
        String channelBrand = channelBrand(raw);
        String cleaned = clean(raw);

        String brand = matchBrand(cleaned);
        if (brand != null) {
            return brand;
        }
        if (channelBrand != null) {
            return channelBrand;
        }
        return cleaned.isBlank() ? raw : cleaned;
    }

    /**
     * 只做渠道前缀/公司后缀/门店后缀清洗，不套品牌别名。
     */
    public String clean(String rawName) {
        if (rawName == null) {
            return "";
        }
        String s = rawName.trim();
        if (s.isEmpty()) {
            return s;
        }

        boolean stripped;
        do {
            stripped = false;
            for (String prefix : PREFIXES) {
                if (s.startsWith(prefix) && s.length() > prefix.length()) {
                    s = s.substring(prefix.length()).trim();
                    stripped = true;
                    break;
                }
            }
        } while (stripped);

        boolean suffixStripped;
        do {
            suffixStripped = false;
            for (String suffix : SUFFIXES) {
                if (s.endsWith(suffix) && s.length() > suffix.length()) {
                    s = s.substring(0, s.length() - suffix.length()).trim();
                    suffixStripped = true;
                    break;
                }
            }
        } while (suffixStripped);

        s = BRANCH_SUFFIX.matcher(s).replaceFirst("").trim();

        for (String suffix : LEGAL_SUFFIXES) {
            if (s.endsWith(suffix) && s.length() > suffix.length()) {
                s = s.substring(0, s.length() - suffix.length()).trim();
                break;
            }
        }

        s = s.replaceAll("[…\\.]+$", "").trim();
        return s;
    }

    private String channelBrand(String raw) {
        for (Map.Entry<String, String> entry : CHANNEL_BRANDS.entrySet()) {
            if (raw.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String matchBrand(String cleaned) {
        if (cleaned == null || cleaned.isBlank()) {
            return null;
        }
        for (BrandAlias alias : BRANDS) {
            if (cleaned.contains(alias.pattern())) {
                return alias.brand();
            }
        }
        return null;
    }

    private static List<BrandAlias> buildBrands() {
        List<BrandAlias> aliases = new ArrayList<>();
        add(aliases, "北京大学软件与微电子学院", "北京大学软件与微电子学院");
        add(aliases, "北京大学口腔医院", "北京大学口腔医院");
        add(aliases, "北京大学", "北京大学");

        add(aliases, "京东大药房", "京东大药房");
        add(aliases, "京东快递", "京东快递");
        add(aliases, "京东服务", "京东服务");
        add(aliases, "京东京造", "京东京造");
        add(aliases, "京东生鲜", "京东生鲜");
        add(aliases, "京东商城", "京东");

        add(aliases, "三快", "美团");
        add(aliases, "美团", "美团");
        add(aliases, "拉扎斯", "饿了么");
        add(aliases, "饿了么", "饿了么");
        add(aliases, "金拱门", "麦当劳");
        add(aliases, "麦当劳", "麦当劳");
        add(aliases, "肯德基", "肯德基");
        add(aliases, "百胜咨询", "百胜餐饮");

        add(aliases, "瑞幸", "瑞幸咖啡");
        add(aliases, "星巴克", "星巴克");
        add(aliases, "盒马", "盒马");
        add(aliases, "山姆", "山姆");
        add(aliases, "沃尔玛", "沃尔玛");
        add(aliases, "永辉", "永辉超市");
        add(aliases, "梦想蜂", "便利蜂");
        add(aliases, "便利蜂", "便利蜂");
        add(aliases, "好邻居", "好邻居");
        add(aliases, "友客便利", "友客便利");
        add(aliases, "可好便利", "可好便利店");
        add(aliases, "众邸便利", "众邸便利");
        add(aliases, "果美生鲜", "果美生鲜");
        add(aliases, "果栗园", "果栗园");
        add(aliases, "栗上品", "栗上品");
        add(aliases, "栗首选", "栗首选");
        add(aliases, "薛记炒货", "薛记炒货");
        add(aliases, "紫光园", "紫光园");
        add(aliases, "庆丰", "庆丰");
        add(aliases, "好利来", "好利来");
        add(aliases, "泸溪河", "泸溪河");
        add(aliases, "巴黎贝甜", "巴黎贝甜");
        add(aliases, "元气寿司", "元气寿司");
        add(aliases, "池田寿司", "池田寿司");
        add(aliases, "兰韵臻面", "兰韵臻面");
        add(aliases, "晋宝章", "晋宝章");
        add(aliases, "晋状元", "晋状元");
        add(aliases, "豫石记", "豫石记");
        add(aliases, "马记永", "马记永");
        add(aliases, "玛嘉烈", "玛嘉烈");
        add(aliases, "日升小馆", "日升小馆");
        add(aliases, "松柏常青", "松柏常青");
        add(aliases, "蝎王府", "蝎王府");
        add(aliases, "探鱼", "探鱼");
        add(aliases, "快乐蜂", "快乐蜂");
        add(aliases, "马辈儿", "马辈儿");
        add(aliases, "马华", "马华");
        add(aliases, "福嘟嘟", "福嘟嘟");
        add(aliases, "民福季缘", "民福季缘");
        add(aliases, "金通泰", "金通泰");
        add(aliases, "鲁小鱼", "鲁小鱼");
        add(aliases, "甜哞哞", "甜哞哞");
        add(aliases, "粤华年", "粤华年");
        add(aliases, "全鑫城", "全鑫城");
        add(aliases, "涮肉洪", "涮肉洪");
        add(aliases, "海泰饭店", "海泰饭店");
        add(aliases, "三玛璐", "三玛璐");
        add(aliases, "玫瑰厅", "玫瑰厅");
        add(aliases, "风味小吃街", "风味小吃街");
        add(aliases, "乄卝", "乄卝茶饮");
        add(aliases, "鸡柳大人", "鸡柳大人");
        add(aliases, "郝四哥", "郝四哥");
        add(aliases, "陈臧记", "陈臧记");
        add(aliases, "北平制冰厂", "北平制冰厂");
        add(aliases, "糕莲申", "糕莲申");
        add(aliases, "穆顺佳兴", "穆顺佳兴");
        add(aliases, "老平城小馆", "老平城小馆");
        add(aliases, "龙人居", "龙人居");
        add(aliases, "早阳包子", "早阳包子");
        add(aliases, "老家肉饼", "老家肉饼");
        add(aliases, "宫门口馒头", "宫门口馒头");
        add(aliases, "草鸡蛋", "草鸡蛋");
        add(aliases, "贺兰山餐厅", "贺兰山餐厅");
        add(aliases, "食间饭店", "食间饭店");
        add(aliases, "仙埔里", "仙埔里");
        add(aliases, "梦松小屋", "梦松小屋");
        add(aliases, "旺面耙", "旺面耙蹄花");
        add(aliases, "久久丫", "久久丫");
        add(aliases, "喜识", "喜识");
        add(aliases, "巧克巧蔻", "巧克巧蔻");
        add(aliases, "巧克巧克", "巧克巧克");
        add(aliases, "邵万生", "邵万生");
        add(aliases, "余仁堂", "余仁堂");
        add(aliases, "恒通香港通訊", "恒通香港通讯");

        add(aliases, "中国铁路网络", "12306");
        add(aliases, "12306", "12306");
        add(aliases, "航旅纵横", "航旅纵横");
        add(aliases, "中航信", "航旅纵横");
        add(aliases, "携程", "携程");
        add(aliases, "贝壳租房", "贝壳");
        add(aliases, "贝壳找房", "贝壳");
        add(aliases, "滴滴", "滴滴出行");
        add(aliases, "嘀嘀", "滴滴出行");
        add(aliases, "高德", "高德打车");
        add(aliases, "中国石化", "中国石化");
        add(aliases, "中石化", "中国石化");
        add(aliases, "中国石油", "中国石油");
        add(aliases, "中国联合网络通信", "中国联通");
        add(aliases, "中国联通", "中国联通");
        add(aliases, "中国移动", "中国移动");
        add(aliases, "中移动", "中国移动");
        add(aliases, "中广电移动", "中国广电");
        add(aliases, "中国广电", "中国广电");
        add(aliases, "国网北京", "国家电网");
        add(aliases, "国家电网", "国家电网");
        add(aliases, "北京燃气", "北京燃气");
        add(aliases, "中国光大银行", "光大银行");
        add(aliases, "光大银行", "光大银行");
        add(aliases, "首都国际机场", "首都机场");
        add(aliases, "首都航空", "首都航空");
        add(aliases, "易行出行旅游", "易行旅行");
        add(aliases, "风采航运旅游", "风采航运");
        add(aliases, "远福文旅", "远福文旅");
        add(aliases, "阿里云", "阿里云");
        add(aliases, "火山引擎", "火山引擎");
        add(aliases, "深度求索", "DeepSeek");
        add(aliases, "云上艾珀", "iCloud");
        add(aliases, "迅销", "优衣库");
        add(aliases, "宜家", "宜家");
        add(aliases, "李宁", "李宁");
        add(aliases, "太平鸟", "太平鸟");
        add(aliases, "伊芙丽", "伊芙丽");
        add(aliases, "百丽", "百丽");
        add(aliases, "爱步", "ECCO");
        add(aliases, "浪莎", "浪莎");
        add(aliases, "松山棉店", "松山棉店");
        add(aliases, "周生生", "周生生");
        add(aliases, "鄂尔多斯", "鄂尔多斯");
        add(aliases, "马拉丁", "马拉丁");
        add(aliases, "克履仕", "克履仕");
        add(aliases, "九木杂物社", "九木杂物社");
        add(aliases, "君太百货", "君太百货");
        add(aliases, "菜市口百货", "菜市口百货");
        add(aliases, "首创奥特莱斯", "首创奥特莱斯");
        add(aliases, "房山奥莱", "房山奥莱");
        add(aliases, "balabala", "Balabala");
        add(aliases, "乐友", "乐友");
        add(aliases, "小米", "小米");
        add(aliases, "OpenRouter", "OpenRouter");
        add(aliases, "海王星辰", "海王星辰");
        add(aliases, "高济百康", "高济百康");
        add(aliases, "中港大药房", "中港大药房");
        add(aliases, "大都会人寿", "大都会人寿");
        add(aliases, "MANNINGS", "万宁");
        add(aliases, "AMAZING JESSICA BAKE", "AMAZING JESSICA BAKE");
        add(aliases, "Yoajung", "Yoajung");
        add(aliases, "HongKong Walnut", "HongKong Walnut");
        add(aliases, "Octopus", "八达通");
        add(aliases, "一卡通", "北京一卡通");

        add(aliases, "微信红包", "微信红包");
        add(aliases, "微信转账", "微信转账");
        add(aliases, "群收款", "群收款");
        add(aliases, "扫二维码付款", "扫二维码付款");
        add(aliases, "智能货柜", "智能货柜");
        add(aliases, "特约商户", "特约商户");
        add(aliases, "京东", "京东");
        add(aliases, "抖音", "抖音");
        add(aliases, "云闪付", "云闪付");
        add(aliases, "数字人民币", "数字人民币");
        add(aliases, "信用卡还款", "信用卡还款");

        add(aliases, "浙江天猫", "天猫");
        add(aliases, "天猫", "天猫");
        add(aliases, "淘宝", "淘宝");
        add(aliases, "寻梦信息技术", "拼多多");
        add(aliases, "格物致品", "抖音商城");
        add(aliases, "亚朵星球", "亚朵星球");
        add(aliases, "亚朵", "亚朵");
        add(aliases, "农夫山泉", "农夫山泉");
        add(aliases, "友宝", "友宝");
        add(aliases, "京味飘香", "京味飘香");
        add(aliases, "紫金居", "紫金居");
        add(aliases, "西永栗", "西永栗");
        add(aliases, "超有财", "超有财");
        add(aliases, "干果轩", "干果轩");
        add(aliases, "晨光文具", "晨光文具");
        add(aliases, "小牛电动", "小牛电动");
        add(aliases, "雅迪", "雅迪");
        add(aliases, "玩品汇", "玩品汇");
        add(aliases, "盛世十月", "盛世十月");
        add(aliases, "容茂", "容茂");
        add(aliases, "费饭厨神", "费饭厨神");
        add(aliases, "梦享兔", "梦享兔");
        add(aliases, "途贺晓雷", "途贺晓雷");
        add(aliases, "趣然", "趣然");
        add(aliases, "赤芍", "赤芍");
        add(aliases, "康优宝", "康优宝");
        add(aliases, "盟迪奥", "盟迪奥");
        add(aliases, "禾美非凡", "禾美非凡");
        add(aliases, "铭马", "铭马");
        add(aliases, "柏视", "柏视");
        add(aliases, "中广物业", "中广物业");
        add(aliases, "全来电", "全来电");
        add(aliases, "和颐合", "和颐合");
        add(aliases, "月中月", "月中月");
        add(aliases, "奇趣生活", "奇趣生活");
        add(aliases, "焕金颜", "焕金颜");
        add(aliases, "番茄口袋", "番茄口袋");
        add(aliases, "美食城镖头", "美食城镖头");
        add(aliases, "铁板大鱿鱼", "铁板大鱿鱼");
        add(aliases, "京能热力", "京能热力");
        add(aliases, "享充云", "享充云");
        add(aliases, "乐摩吧", "乐摩吧");
        add(aliases, "西海岸泊车", "西海岸泊车");
        add(aliases, "山东高速", "山东高速");
        add(aliases, "广西捷通", "广西捷通");

        aliases.sort(Comparator.comparingInt((BrandAlias a) -> a.pattern().length()).reversed());
        return List.copyOf(aliases);
    }

    private static void add(List<BrandAlias> aliases, String pattern, String brand) {
        aliases.add(new BrandAlias(pattern, brand));
    }

    private record BrandAlias(String pattern, String brand) {
    }
}