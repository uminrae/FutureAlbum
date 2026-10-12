package com.example.phototag.util.exif

/**
 * 伪装地理位置预设模型
 */
data class SpoofLocationPreset(
    val id: String,
    val name: String,
    val region: String, // "韩国主要城市", "全球著名地标", "自定义"
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 35.0,
    val displayAddress: String,
    val iconEmoji: String
)

/**
 * 伪装设备机型预设模型
 */
data class SpoofDevicePreset(
    val id: String,
    val name: String,
    val brand: String, // "Apple", "Samsung", "专业相机", "自定义"
    val make: String,
    val model: String,
    val software: String
)

/**
 * 时间伪装选项
 */
enum class SpoofTimeMode(val label: String) {
    KEEP_ORIGINAL("保持原图时间"),
    JUST_NOW("刚刚 / 当前时间"),
    YESTERDAY("昨天同时间"),
    THREE_DAYS_AGO("3 天前"),
    ONE_WEEK_AGO("1 周前"),
    ONE_MONTH_AGO("1 个月前"),
    CUSTOM("自定义具体时间")
}

/**
 * 完整伪装配置对象
 */
data class SpoofConfig(
    val enableLocation: Boolean = true,
    val locationName: String = "首尔塔 · 南山",
    val latitude: Double = 37.5512,
    val longitude: Double = 126.9882,
    val altitude: Double = 243.0,
    val displayAddress: String = "首尔特别市龙山区南山公园路105 (首尔塔)",
    
    val enableDevice: Boolean = true,
    val deviceName: String = "Apple iPhone 16 Pro Max",
    val make: String = "Apple",
    val model: String = "iPhone 16 Pro Max",
    val software: String = "iOS 18.1",
    
    val enableTime: Boolean = false,
    val timeMode: SpoofTimeMode = SpoofTimeMode.KEEP_ORIGINAL,
    val customDateMs: Long? = null
)

object ExifSpoofPresets {

    // ==================== 韩国热门商圈与知名地标 ====================
    val KOREA_LOCATIONS = listOf(
        SpoofLocationPreset(
            id = "kr_hannam_dong",
            name = "汉南洞 · 潮流买手街",
            region = "首尔特别市",
            latitude = 37.5340,
            longitude = 127.0026,
            altitude = 45.0,
            displayAddress = "首尔特别市龙山区梨泰院路54街 (汉南洞潮流街区)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_seongsu_dong",
            name = "圣水洞 · 咖啡快闪街",
            region = "首尔特别市",
            latitude = 37.5446,
            longitude = 127.0560,
            altitude = 25.0,
            displayAddress = "首尔特别市城东区演武场街 (圣水洞咖啡文创区)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_apgujeong_rodeo",
            name = "狎鸥亭罗德奥 · 清潭洞",
            region = "首尔特别市",
            latitude = 37.5267,
            longitude = 127.0384,
            altitude = 42.0,
            displayAddress = "首尔特别市江南区狎鸥亭路50街 (狎鸥亭罗德奥)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_the_hyundai_seoul",
            name = "汝矣岛 · 现代百货",
            region = "首尔特别市",
            latitude = 37.5259,
            longitude = 126.9284,
            altitude = 18.0,
            displayAddress = "首尔特别市永登浦区汝矣大路108 (The Hyundai Seoul)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_seoul_myeongdong",
            name = "明洞商业街 · 免税店",
            region = "首尔特别市",
            latitude = 37.5636,
            longitude = 126.9827,
            altitude = 38.0,
            displayAddress = "首尔特别市中区明洞8街27 (乐天免税店/明洞)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_seoul_hongdae",
            name = "弘益大学 · 弘大商圈",
            region = "首尔特别市",
            latitude = 37.5563,
            longitude = 126.9226,
            altitude = 30.0,
            displayAddress = "首尔特别市麻浦区弘益路 (弘大步行街)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_garosugil",
            name = "新沙洞 · 林荫道",
            region = "首尔特别市",
            latitude = 37.5204,
            longitude = 127.0229,
            altitude = 36.0,
            displayAddress = "首尔特别市江南区江南大路152街 (林荫道)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_lotte_world_tower",
            name = "乐天世界塔 · 蚕室",
            region = "首尔特别市",
            latitude = 37.5126,
            longitude = 127.1025,
            altitude = 555.0,
            displayAddress = "首尔特别市松坡区奥林匹克路300 (乐天世界塔)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_ddp_dongdaemun",
            name = "东大门设计广场 · DDP",
            region = "首尔特别市",
            latitude = 37.5665,
            longitude = 127.0092,
            altitude = 35.0,
            displayAddress = "首尔特别市中区乙支路281 (DDP东大门设计广场)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_itaewon",
            name = "梨泰院 · 潮流美食街",
            region = "首尔特别市",
            latitude = 37.5345,
            longitude = 126.9946,
            altitude = 48.0,
            displayAddress = "首尔特别市龙山区梨泰院路179",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_seoul_palace",
            name = "景福宫 · 光化门广场",
            region = "首尔特别市",
            latitude = 37.5796,
            longitude = 126.9770,
            altitude = 45.0,
            displayAddress = "首尔特别市钟路区社稷路161 (景福宫)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_seoul_tower",
            name = "首尔塔 · 南山公园",
            region = "首尔特别市",
            latitude = 37.5512,
            longitude = 126.9882,
            altitude = 243.0,
            displayAddress = "首尔特别市龙山区南山公园路105 (首尔塔)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_busan_centum",
            name = "釜山 · 新世界 Centum City",
            region = "釜山广域市",
            latitude = 35.1689,
            longitude = 129.1298,
            altitude = 12.0,
            displayAddress = "釜山广域市海云台区Centum4路15 (新世界百货)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_busan_seomyeon",
            name = "釜山 · 西面商圈",
            region = "釜山广域市",
            latitude = 35.1578,
            longitude = 129.0592,
            altitude = 15.0,
            displayAddress = "釜山广域市釜山镇区中央大路730 (西面商业街)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_busan_haeundae",
            name = "釜山 · 海云台海滩",
            region = "釜山广域市",
            latitude = 35.1587,
            longitude = 129.1604,
            altitude = 5.0,
            displayAddress = "釜山广域市海云台区海云台海边路264",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_busan_gwanganri",
            name = "釜山 · 广安里大桥海滩",
            region = "釜山广域市",
            latitude = 35.1532,
            longitude = 129.1189,
            altitude = 6.0,
            displayAddress = "釜山广域市水营区广安海边路219",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_jeju_aewol",
            name = "济州岛 · 涯月邑海边咖啡街",
            region = "济州特别自治道",
            latitude = 33.4655,
            longitude = 126.3194,
            altitude = 12.0,
            displayAddress = "济州特别自治道济州市涯月邑涯月路1街",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_jeju_dongmun",
            name = "济州岛 · 七星街 / 东门市场",
            region = "济州特别自治道",
            latitude = 33.5133,
            longitude = 126.5273,
            altitude = 20.0,
            displayAddress = "济州特别自治道济州市观德路14街 (东门市场)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_jeju_seongsan",
            name = "济州岛 · 城山日出峰",
            region = "济州特别自治道",
            latitude = 33.4581,
            longitude = 126.9426,
            altitude = 182.0,
            displayAddress = "济州特别自治道西归浦市城山邑日出路284-12",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_suwon_hwaseong",
            name = "水原市 · 水原华城",
            region = "京畿道水原市",
            latitude = 37.2872,
            longitude = 127.0119,
            altitude = 85.0,
            displayAddress = "京畿道水原市八达区正祖路910 (水原华城)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "kr_seongnam_pangyo",
            name = "城南市 · 板桥科技谷",
            region = "京畿道城南市",
            latitude = 37.3948,
            longitude = 127.1119,
            altitude = 55.0,
            displayAddress = "京畿道城南市盆唐区大旺板桥路645号街 (现代百货板桥店)",
            iconEmoji = ""
        )
    )

    // ==================== 日本热门商圈与知名地标 ====================
    val JAPAN_LOCATIONS = listOf(
        SpoofLocationPreset(
            id = "jp_tokyo_shibuya",
            name = "东京 · 涩谷 Scramble 商业街",
            region = "东京都涩谷区",
            latitude = 35.6595,
            longitude = 139.7004,
            altitude = 35.0,
            displayAddress = "2 Chome-24-12 Shibuya, Shibuya City, Tokyo 150-0002, Japan (涩谷Scramble Square)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_tokyo_shinjuku",
            name = "东京 · 新宿伊势丹 / 歌舞伎町",
            region = "东京都新宿区",
            latitude = 35.6917,
            longitude = 139.7046,
            altitude = 40.0,
            displayAddress = "3 Chome-14-1 Shinjuku, Shinjuku City, Tokyo 160-0022, Japan (伊势丹新宿店)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_tokyo_ginza",
            name = "东京 · 银座 GINZA SIX",
            region = "东京都中央区",
            latitude = 35.6712,
            longitude = 139.7650,
            altitude = 20.0,
            displayAddress = "6 Chome-10-1 Ginza, Chuo City, Tokyo 104-0061, Japan (GINZA SIX)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_tokyo_omotesando",
            name = "东京 · 原宿表参道之丘",
            region = "东京都涩谷区",
            latitude = 35.6668,
            longitude = 139.7093,
            altitude = 36.0,
            displayAddress = "4 Chome-12-10 Jingumae, Shibuya City, Tokyo 150-0001, Japan (表参道Hills)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_tokyo_akihabara",
            name = "东京 · 秋叶原电气街",
            region = "东京都千代田区",
            latitude = 35.6983,
            longitude = 139.7731,
            altitude = 22.0,
            displayAddress = "1 Chome Sotokanda, Chiyoda City, Tokyo 101-0021, Japan (秋叶原)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_tokyo_roppongi",
            name = "东京 · 六本木之丘",
            region = "东京都港区",
            latitude = 35.6605,
            longitude = 139.7292,
            altitude = 52.0,
            displayAddress = "6 Chome-10-1 Roppongi, Minato City, Tokyo 106-0032, Japan (六本木Hills)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_tokyo_shimokitazawa",
            name = "东京 · 下北泽古着买手街",
            region = "东京都世田谷区",
            latitude = 35.6616,
            longitude = 139.6667,
            altitude = 45.0,
            displayAddress = "2 Chome Kitazawa, Setagaya City, Tokyo 155-0031, Japan (下北泽)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_tokyo_sunshine",
            name = "东京 · 池袋太阳城",
            region = "东京都丰岛区",
            latitude = 35.7289,
            longitude = 139.7186,
            altitude = 38.0,
            displayAddress = "3 Chome-1-1 Higashiikebukuro, Toshima City, Tokyo 170-8630, Japan",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_osaka_shinsaibashi",
            name = "大阪 · 心斋桥筋商业街",
            region = "大阪府大阪市",
            latitude = 34.6710,
            longitude = 135.5013,
            altitude = 12.0,
            displayAddress = "2 Chome Shinsaibashisuji, Chuo Ward, Osaka 542-0085, Japan (心斋桥筋)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_osaka_umeda",
            name = "大阪 · 梅田阪急百货",
            region = "大阪府大阪市",
            latitude = 34.7025,
            longitude = 135.4983,
            altitude = 15.0,
            displayAddress = "8-7 Kakudacho, Kita Ward, Osaka 530-8350, Japan (阪急百货梅田总店)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_osaka_dotonbori",
            name = "大阪 · 道顿堀美食街",
            region = "大阪府大阪市",
            latitude = 34.6687,
            longitude = 135.5013,
            altitude = 10.0,
            displayAddress = "1 Chome Dotonbori, Chuo Ward, Osaka 542-0071, Japan (道顿堀)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_osaka_namba",
            name = "大阪 · 难波 Parks 购物中心",
            region = "大阪府大阪市",
            latitude = 34.6618,
            longitude = 135.5020,
            altitude = 14.0,
            displayAddress = "2 Chome-10-70 Nanbanaka, Naniwa Ward, Osaka 556-0011, Japan (难波Parks)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_kyoto_kawaramachi",
            name = "京都 · 四条河原町 / 锦市场",
            region = "京都府京都市",
            latitude = 35.0037,
            longitude = 135.7694,
            altitude = 48.0,
            displayAddress = "Shijo Dori Kawaramachi, Shimogyo Ward, Kyoto 600-8001, Japan (四条河原町)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_kyoto_kiyomizu",
            name = "京都 · 清水寺二年坂 / 三年坂",
            region = "京都府京都市",
            latitude = 34.9949,
            longitude = 135.7850,
            altitude = 150.0,
            displayAddress = "1 Chome-294 Kiyomizu, Higashiyama Ward, Kyoto 605-0862, Japan (清水寺)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_fukuoka_tenjin",
            name = "福冈 · 天神地下街 / 博多运河城",
            region = "福冈县福冈市",
            latitude = 33.5897,
            longitude = 130.4108,
            altitude = 8.0,
            displayAddress = "1 Chome-2 Sumiyoshi, Hakata Ward, Fukuoka 812-0018, Japan (博多运河城)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_nagoya_sakae",
            name = "名古屋 · 荣商圈 / 松坂屋百货",
            region = "爱知县名古屋市",
            latitude = 35.1691,
            longitude = 136.9083,
            altitude = 18.0,
            displayAddress = "3 Chome Sakae, Naka Ward, Nagoya, Aichi 460-0008, Japan (荣商圈)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "jp_fuji",
            name = "日本 · 富士山五合目",
            region = "静冈县富士宫市",
            latitude = 35.3606,
            longitude = 138.7274,
            altitude = 2305.0,
            displayAddress = "Kitayama, Fujinomiya, Shizuoka 418-0112, Japan",
            iconEmoji = ""
        )
    )

    // ==================== 国内热门商圈与时尚地标 ====================
    val CHINA_LOCATIONS = listOf(
        SpoofLocationPreset(
            id = "cn_sh_anfu",
            name = "上海 · 安福路 / 武康路",
            region = "上海市徐汇区",
            latitude = 31.2138,
            longitude = 121.4428,
            altitude = 15.0,
            displayAddress = "上海市徐汇区安福路 (武康路/安福路潮流街区)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_sh_jingan",
            name = "上海 · 静安寺 / 恒隆广场",
            region = "上海市静安区",
            latitude = 31.2268,
            longitude = 121.4475,
            altitude = 18.0,
            displayAddress = "上海市静安区南京西路1266号 (恒隆广场)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_sh_ifc",
            name = "上海 · 陆家嘴 / 国金中心 IFC",
            region = "上海市浦东新区",
            latitude = 31.2397,
            longitude = 121.5002,
            altitude = 22.0,
            displayAddress = "上海市浦东新区世纪大道8号 (上海国金中心IFC)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_sh_xintiandi",
            name = "上海 · 新天地 / 淮海中路",
            region = "上海市黄浦区",
            latitude = 31.2208,
            longitude = 121.4746,
            altitude = 16.0,
            displayAddress = "上海市黄浦区太仓路181弄 (新天地南里)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_sh_qiantan",
            name = "上海 · 前滩太古里",
            region = "上海市浦东新区",
            latitude = 31.1578,
            longitude = 121.4829,
            altitude = 14.0,
            displayAddress = "上海市浦东新区东育路500号 (前滩太古里)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_bj_sanlitun",
            name = "北京 · 三里屯太古里",
            region = "北京市朝阳区",
            latitude = 39.9348,
            longitude = 116.4552,
            altitude = 42.0,
            displayAddress = "北京市朝阳区三里屯路19号 (三里屯太古里)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_bj_skp",
            name = "北京 · SKP / 华贸中心",
            region = "北京市朝阳区",
            latitude = 39.9102,
            longitude = 116.4741,
            altitude = 40.0,
            displayAddress = "北京市朝阳区建国路87号 (北京SKP百货)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_bj_wangfujing",
            name = "北京 · 王府井步行街 / 中环王府井",
            region = "北京市东城区",
            latitude = 39.9145,
            longitude = 116.4116,
            altitude = 45.0,
            displayAddress = "北京市东城区王府井大街255号 (王府井百货)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_bj_798",
            name = "北京 · 颐堤港 / 798 艺术区",
            region = "北京市朝阳区",
            latitude = 39.9842,
            longitude = 116.4952,
            altitude = 43.0,
            displayAddress = "北京市朝阳区酒仙桥路4号 (798艺术区)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_sz_mixc_world",
            name = "深圳 · 万象天地",
            region = "深圳市南山区",
            latitude = 22.5408,
            longitude = 113.9547,
            altitude = 28.0,
            displayAddress = "深圳市南山区深南大道9668号 (深圳万象天地)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_sz_bay_mixc",
            name = "深圳 · 深圳湾万象城",
            region = "深圳市南山区",
            latitude = 22.5186,
            longitude = 113.9472,
            altitude = 15.0,
            displayAddress = "深圳市南山区科苑南路2888号 (深圳湾万象城)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_sz_luohu_mixc",
            name = "深圳 · 罗湖万象城 / 东门步行街",
            region = "深圳市罗湖区",
            latitude = 22.5385,
            longitude = 114.1118,
            altitude = 18.0,
            displayAddress = "深圳市罗湖区宝安南路1881号 (深圳万象城)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_gz_taikoo",
            name = "广州 · 天河城 / 太古汇",
            region = "广州市天河区",
            latitude = 23.1344,
            longitude = 113.3328,
            altitude = 25.0,
            displayAddress = "广州市天河区天河路383号 (广州太古汇)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_gz_k11",
            name = "广州 · 珠江新城 / K11 艺术购物中心",
            region = "广州市天河区",
            latitude = 23.1192,
            longitude = 113.3213,
            altitude = 22.0,
            displayAddress = "广州市天河区珠江东路6号 (广州K11购物艺术中心)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_gz_dongshankou",
            name = "广州 · 东山口潮流街区",
            region = "广州市越秀区",
            latitude = 23.1258,
            longitude = 113.2952,
            altitude = 28.0,
            displayAddress = "广州市越秀区庙前西街 (东山口文创商圈)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_cd_taikooli",
            name = "成都 · 太古里 / 春熙路",
            region = "成都市锦江区",
            latitude = 30.6558,
            longitude = 104.0818,
            altitude = 500.0,
            displayAddress = "成都市锦江区中纱帽街8号 (成都太古里)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_cd_ifs",
            name = "成都 · IFS 国际金融中心",
            region = "成都市锦江区",
            latitude = 30.6565,
            longitude = 104.0810,
            altitude = 500.0,
            displayAddress = "成都市锦江区红星路三段1号 (成都IFS)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_hz_in77",
            name = "杭州 · 湖滨银泰 in77",
            region = "杭州市上城区",
            latitude = 30.2541,
            longitude = 120.1619,
            altitude = 19.0,
            displayAddress = "杭州市上城区延安路258号 (湖滨银泰in77)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_hz_tianmuli",
            name = "杭州 · 天目里",
            region = "杭州市西湖区",
            latitude = 30.2642,
            longitude = 120.0819,
            altitude = 22.0,
            displayAddress = "杭州市西湖区天目山路398号 (天目里艺术园区)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_cq_jiefangbei",
            name = "重庆 · 解放碑 / 时代广场",
            region = "重庆市渝中区",
            latitude = 29.5571,
            longitude = 106.5771,
            altitude = 260.0,
            displayAddress = "重庆市渝中区民族路177号 (解放碑商业步行街)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_cq_guanyinqiao",
            name = "重庆 · 观音桥步行街 / 北城天街",
            region = "重庆市江北区",
            latitude = 29.5772,
            longitude = 106.5332,
            altitude = 280.0,
            displayAddress = "重庆市江北区观音桥步行街 (北城天街)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_wh_wushang",
            name = "武汉 · 武商 MALL / 国广",
            region = "武汉市江汉区",
            latitude = 30.5843,
            longitude = 114.2706,
            altitude = 32.0,
            displayAddress = "武汉市江汉区解放大道690号 (武商MALL)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "cn_nj_deji",
            name = "南京 · 新街口德基广场",
            region = "南京市玄武区",
            latitude = 32.0438,
            longitude = 118.7845,
            altitude = 26.0,
            displayAddress = "南京市玄武区中山路18号 (德基广场)",
            iconEmoji = ""
        )
    )

    // ==================== 国际经典名胜与购物殿堂 ====================
    val GLOBAL_LOCATIONS = listOf(
        SpoofLocationPreset(
            id = "global_paris_lafayette",
            name = "巴黎 · 香榭丽舍 / 老佛爷百货",
            region = "法国巴黎",
            latitude = 48.8708,
            longitude = 2.3082,
            altitude = 45.0,
            displayAddress = "40 Bd Haussmann, 75009 Paris, France (老佛爷百货)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "global_paris_eiffel",
            name = "巴黎 · 埃菲尔铁塔",
            region = "法国巴黎",
            latitude = 48.8584,
            longitude = 2.2945,
            altitude = 300.0,
            displayAddress = "Champ de Mars, 5 Av. Anatole France, 75007 Paris, France",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "global_london_harrods",
            name = "伦敦 · 摄政街 / 哈罗德百货",
            region = "英国伦敦",
            latitude = 51.4998,
            longitude = -0.1634,
            altitude = 25.0,
            displayAddress = "87-135 Brompton Rd, London SW1X 7XL, UK (Harrods)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "global_ny_5th_ave",
            name = "纽约 · 第五大道",
            region = "美国纽约",
            latitude = 40.7618,
            longitude = -73.9744,
            altitude = 30.0,
            displayAddress = "767 5th Ave, New York, NY 10153, USA (Fifth Avenue)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "global_hk_harbour_city",
            name = "香港 · 尖沙咀海港城",
            region = "中国香港",
            latitude = 22.2988,
            longitude = 114.1685,
            altitude = 10.0,
            displayAddress = "香港九龙尖沙咀广东道3-27号 (海港城)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "global_macau_venetian",
            name = "澳门 · 威尼斯人 / 伦敦人商圈",
            region = "中国澳门",
            latitude = 22.1466,
            longitude = 113.5604,
            altitude = 12.0,
            displayAddress = "澳门路氹连贯公路 (澳门威尼斯人度假村)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "global_bkk_siam_paragon",
            name = "曼谷 · 暹罗百丽宫",
            region = "泰国曼谷",
            latitude = 13.7466,
            longitude = 100.5348,
            altitude = 15.0,
            displayAddress = "991 Rama I Rd, Pathum Wan, Bangkok 10330, Thailand (Siam Paragon)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "global_sg_ion_orchard",
            name = "新加坡 · 乌节路 ION Orchard",
            region = "新加坡",
            latitude = 1.3040,
            longitude = 103.8318,
            altitude = 20.0,
            displayAddress = "2 Orchard Turn, Singapore 238801 (ION Orchard)",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "global_iceland",
            name = "冰岛 · 雷克雅未克极光",
            region = "冰岛黄金圈",
            latitude = 64.1466,
            longitude = -21.9426,
            altitude = 40.0,
            displayAddress = "Reykjavík, Capital Region, Iceland",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "global_hawaii",
            name = "夏威夷 · 威基基海滩",
            region = "美国夏威夷",
            latitude = 21.2766,
            longitude = -157.8273,
            altitude = 3.0,
            displayAddress = "Waikiki Beach, Honolulu, HI 96815, USA",
            iconEmoji = ""
        ),
        SpoofLocationPreset(
            id = "global_antarctica",
            name = "南极 · 阿蒙森-斯科特科考站",
            region = "南极洲极点",
            latitude = -89.9999,
            longitude = 0.0000,
            altitude = 2835.0,
            displayAddress = "Amundsen-Scott South Pole Station, Antarctica",
            iconEmoji = ""
        )
    )

    // ==================== Apple 苹果旗舰机型 ====================
    val APPLE_DEVICES = listOf(
        SpoofDevicePreset(
            id = "apple_16pm",
            name = "iPhone 16 Pro Max",
            brand = "Apple",
            make = "Apple",
            model = "iPhone 16 Pro Max",
            software = "iOS 18.1"
        ),
        SpoofDevicePreset(
            id = "apple_16p",
            name = "iPhone 16 Pro",
            brand = "Apple",
            make = "Apple",
            model = "iPhone 16 Pro",
            software = "iOS 18.1"
        ),
        SpoofDevicePreset(
            id = "apple_15pm",
            name = "iPhone 15 Pro Max",
            brand = "Apple",
            make = "Apple",
            model = "iPhone 15 Pro Max",
            software = "iOS 17.6.1"
        ),
        SpoofDevicePreset(
            id = "apple_15p",
            name = "iPhone 15 Pro",
            brand = "Apple",
            make = "Apple",
            model = "iPhone 15 Pro",
            software = "iOS 17.6.1"
        ),
        SpoofDevicePreset(
            id = "apple_14pm",
            name = "iPhone 14 Pro Max",
            brand = "Apple",
            make = "Apple",
            model = "iPhone 14 Pro Max",
            software = "iOS 16.5"
        )
    )

    // ==================== Samsung 三星 Galaxy 旗舰机型 ====================
    val SAMSUNG_DEVICES = listOf(
        SpoofDevicePreset(
            id = "samsung_s24u",
            name = "Galaxy S24 Ultra",
            brand = "Samsung",
            make = "samsung",
            model = "SM-S9280",
            software = "One UI 6.1.1"
        ),
        SpoofDevicePreset(
            id = "samsung_s24p",
            name = "Galaxy S24+",
            brand = "Samsung",
            make = "samsung",
            model = "SM-S9260",
            software = "One UI 6.1.1"
        ),
        SpoofDevicePreset(
            id = "samsung_s24",
            name = "Galaxy S24",
            brand = "Samsung",
            make = "samsung",
            model = "SM-S9210",
            software = "One UI 6.1"
        ),
        SpoofDevicePreset(
            id = "samsung_zfold6",
            name = "Galaxy Z Fold6",
            brand = "Samsung",
            make = "samsung",
            model = "SM-F9560",
            software = "One UI 6.1.1"
        ),
        SpoofDevicePreset(
            id = "samsung_zflip6",
            name = "Galaxy Z Flip6",
            brand = "Samsung",
            make = "samsung",
            model = "SM-F7410",
            software = "One UI 6.1.1"
        ),
        SpoofDevicePreset(
            id = "samsung_s23u",
            name = "Galaxy S23 Ultra",
            brand = "Samsung",
            make = "samsung",
            model = "SM-S9180",
            software = "One UI 6.1"
        )
    )

    // ==================== 经典专业相机 ====================
    val CAMERA_DEVICES = listOf(
        SpoofDevicePreset(
            id = "cam_leica_m11",
            name = "Leica M11-P",
            brand = "专业相机",
            make = "LEICA CAMERA AG",
            model = "LEICA M11-P",
            software = "Firmware 2.1.1"
        ),
        SpoofDevicePreset(
            id = "cam_hasselblad_x2d",
            name = "Hasselblad X2D 100C",
            brand = "专业相机",
            make = "Hasselblad",
            model = "X2D 100C",
            software = "v2.0.0"
        ),
        SpoofDevicePreset(
            id = "cam_fuji_x100vi",
            name = "FUJIFILM X100VI",
            brand = "专业相机",
            make = "FUJIFILM",
            model = "X100VI",
            software = "Ver.1.10"
        ),
        SpoofDevicePreset(
            id = "cam_sony_a7r5",
            name = "SONY ILCE-7RM5 (A7R5)",
            brand = "专业相机",
            make = "SONY",
            model = "ILCE-7RM5",
            software = "ILCE-7RM5 v2.00"
        )
    )

    /** 全量预设地标合集 */
    val ALL_LOCATIONS: List<SpoofLocationPreset> by lazy {
        KOREA_LOCATIONS + JAPAN_LOCATIONS + CHINA_LOCATIONS + GLOBAL_LOCATIONS
    }

    /**
     * 根据经纬度在预设地标库中快速匹配（误差在 0.005 度以内，约 500 米）
     */
    fun findMatchingPreset(lat: Double, lng: Double): SpoofLocationPreset? {
        return ALL_LOCATIONS.firstOrNull { preset ->
            kotlin.math.abs(preset.latitude - lat) < 0.005 &&
            kotlin.math.abs(preset.longitude - lng) < 0.005
        }
    }
}