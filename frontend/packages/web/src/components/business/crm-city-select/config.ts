import { mapTree } from '@lib/shared/method';

import { provinceAndCityData, regionData } from 'element-china-area-data';
import type { CascaderOption } from 'naive-ui';

export const GAT = [
  {
    children: [
      {
        children: [
          {
            label: '中正区',
            value: '710101',
          },
          {
            label: '大同区',
            value: '710102',
          },
          {
            label: '中山区',
            value: '710103',
          },
          {
            label: '松山区',
            value: '710104',
          },
          {
            label: '大安区',
            value: '710105',
          },
          {
            label: '万华区',
            value: '710106',
          },
          {
            label: '信义区',
            value: '710107',
          },
          {
            label: '士林区',
            value: '710108',
          },
          {
            label: '北投区',
            value: '710109',
          },
          {
            label: '内湖区',
            value: '710110',
          },
          {
            label: '南港区',
            value: '710111',
          },
          {
            label: '文山区',
            value: '710112',
          },
          {
            label: '其它区',
            value: '710113',
          },
        ],
        label: '台北市',
        value: '7101',
      },
      {
        children: [
          {
            label: '新兴区',
            value: '710201',
          },
          {
            label: '前金区',
            value: '710202',
          },
          {
            label: '芩雅区',
            value: '710203',
          },
          {
            label: '盐埕区',
            value: '710204',
          },
          {
            label: '鼓山区',
            value: '710205',
          },
          {
            label: '旗津区',
            value: '710206',
          },
          {
            label: '前镇区',
            value: '710207',
          },
          {
            label: '三民区',
            value: '710208',
          },
          {
            label: '左营区',
            value: '710209',
          },
          {
            label: '楠梓区',
            value: '710210',
          },
          {
            label: '小港区',
            value: '710211',
          },
          {
            label: '其它区',
            value: '710212',
          },
        ],
        label: '高雄市',
        value: '7102',
      },
      {
        children: [
          {
            label: '中西区',
            value: '710301',
          },
          {
            label: '东区',
            value: '710302',
          },
          {
            label: '南区',
            value: '710303',
          },
          {
            label: '北区',
            value: '710304',
          },
          {
            label: '安平区',
            value: '710305',
          },
          {
            label: '安南区',
            value: '710306',
          },
          {
            label: '其它区',
            value: '710307',
          },
        ],
        label: '台南市',
        value: '7103',
      },
      {
        children: [
          {
            label: '中区',
            value: '710401',
          },
          {
            label: '东区',
            value: '710402',
          },
          {
            label: '南区',
            value: '710403',
          },
          {
            label: '西区',
            value: '710404',
          },
          {
            label: '北区',
            value: '710405',
          },
          {
            label: '北屯区',
            value: '710406',
          },
          {
            label: '西屯区',
            value: '710407',
          },
          {
            label: '南屯区',
            value: '710408',
          },
          {
            label: '其它区',
            value: '710409',
          },
        ],
        label: '台中市',
        value: '7104',
      },
      {
        label: '金门县',
        value: '7105',
      },
      {
        label: '南投县',
        value: '7106',
      },
      {
        children: [
          {
            label: '仁爱区',
            value: '710701',
          },
          {
            label: '信义区',
            value: '710702',
          },
          {
            label: '中正区',
            value: '710703',
          },
          {
            label: '中山区',
            value: '710704',
          },
          {
            label: '安乐区',
            value: '710705',
          },
          {
            label: '暖暖区',
            value: '710706',
          },
          {
            label: '七堵区',
            value: '710707',
          },
          {
            label: '其它区',
            value: '710708',
          },
        ],
        label: '基隆市',
        value: '7107',
      },
      {
        children: [
          {
            label: '东区',
            value: '710801',
          },
          {
            label: '北区',
            value: '710802',
          },
          {
            label: '香山区',
            value: '710803',
          },
          {
            label: '其它区',
            value: '710804',
          },
        ],
        label: '新竹市',
        value: '7108',
      },
      {
        children: [
          {
            label: '东区',
            value: '710901',
          },
          {
            label: '西区',
            value: '710902',
          },
          {
            label: '其它区',
            value: '710903',
          },
        ],
        label: '嘉义市',
        value: '7109',
      },
      {
        label: '新北市',
        value: '7111',
      },
      {
        label: '宜兰县',
        value: '7112',
      },
      {
        label: '新竹县',
        value: '7113',
      },
      {
        label: '桃园县',
        value: '7114',
      },
      {
        label: '苗栗县',
        value: '7115',
      },
      {
        label: '彰化县',
        value: '7117',
      },
      {
        label: '嘉义县',
        value: '7119',
      },
      {
        label: '云林县',
        value: '7121',
      },
      {
        label: '屏东县',
        value: '7124',
      },
      {
        label: '台东县',
        value: '7125',
      },
      {
        label: '花莲县',
        value: '7126',
      },
      {
        label: '澎湖县',
        value: '7127',
      },
    ],
    label: '台湾省',
    value: '71',
  },
  {
    children: [
      {
        children: [
          {
            label: '中西区',
            value: '810101',
          },
          {
            label: '湾仔',
            value: '810102',
          },
          {
            label: '东区',
            value: '810103',
          },
          {
            label: '南区',
            value: '810104',
          },
        ],
        label: '香港岛',
        value: '8101',
      },
      {
        children: [
          {
            label: '九龙城区',
            value: '810201',
          },
          {
            label: '油尖旺区',
            value: '810202',
          },
          {
            label: '深水埗区',
            value: '810203',
          },
          {
            label: '黄大仙区',
            value: '810204',
          },
          {
            label: '观塘区',
            value: '810205',
          },
        ],
        label: '九龙',
        value: '8102',
      },
      {
        children: [
          {
            label: '北区',
            value: '810301',
          },
          {
            label: '大埔区',
            value: '810302',
          },
          {
            label: '沙田区',
            value: '810303',
          },
          {
            label: '西贡区',
            value: '810304',
          },
          {
            label: '元朗区',
            value: '810305',
          },
          {
            label: '屯门区',
            value: '810306',
          },
          {
            label: '荃湾区',
            value: '810307',
          },
          {
            label: '葵青区',
            value: '810308',
          },
          {
            label: '离岛区',
            value: '810309',
          },
        ],
        label: '新界',
        value: '8103',
      },
    ],
    label: '香港特别行政区',
    value: '81',
  },
  {
    children: [
      {
        children: [
          {
            label: '大堂区',
            value: '820100',
          },
          {
            label: '风顺堂区',
            value: '820100',
          },
          {
            label: '花地玛堂区',
            value: '820100',
          },
          {
            label: '花王堂区',
            value: '820100',
          },
          {
            label: '望德堂区',
            value: '820100',
          },
          {
            label: '其他区',
            value: '820100',
          },
        ],
        label: '澳门半岛',
        value: '8201',
      },
      {
        children: [
          {
            label: '嘉模堂区',
            value: '820200',
          },
          {
            label: '路凼填海区',
            value: '820200',
          },
          {
            label: '圣方济各堂区',
            value: '820200',
          },
          {
            label: '其他区',
            value: '820200',
          },
        ],
        label: '离岛',
        value: '8202',
      },
    ],
    label: '澳门特别行政区',
    value: '82',
  },
];

export const GAT_PC = mapTree(GAT, (item, path, level) => {
  if (level === 1) {
    return {
      ...item,
      children: undefined,
    };
  }
  return item;
});

export const CHINA_PC = {
  label: '中国',
  value: 'CHN',
  children: [...provinceAndCityData, ...GAT_PC],
};

export const CHINA_PCD = {
  label: '中国',
  value: 'CHN',
  children: [...regionData, ...GAT],
};

export const CHINA_P = {
  label: '中国',
  value: 'CHN',
  children: [...regionData, ...GAT].map(({ label, value }) => ({ label, value })),
};

export const COUNTRIES_TREE: CascaderOption[] = [
  {
    label: '阿鲁巴',
    value: 'ABW',
  },
  {
    label: '阿富汗',
    value: 'AFG',
    children: [
      {
        label: '赫拉特',
        value: 'AFG_HEA',
      },
      {
        label: '喀布尔',
        value: 'AFG_KBL',
      },
      {
        label: '坎大哈',
        value: 'AFG_KDH',
      },
      {
        label: '马扎里沙里夫',
        value: 'AFG_MZR',
      },
    ],
  },
  {
    label: '安圭拉',
    value: 'AIA',
  },
  {
    label: '奥兰群岛',
    value: 'ALA',
  },
  {
    label: '阿尔巴尼亚',
    value: 'ALB',
    children: [
      {
        label: '爱尔巴桑',
        value: 'ALB_EL',
      },
      {
        label: '迪勃拉',
        value: 'ALB_DI',
      },
      {
        label: '地拉那',
        value: 'ALB_TR',
      },
      {
        label: '都拉斯',
        value: 'ALB_DR',
      },
      {
        label: '发罗拉',
        value: 'ALB_VL',
      },
      {
        label: '费里',
        value: 'ALB_FR',
      },
      {
        label: '吉诺卡斯特',
        value: 'ALB_GJ',
      },
      {
        label: '科尔察',
        value: 'ALB_KO',
      },
      {
        label: '库克斯',
        value: 'ALB_KU',
      },
      {
        label: '莱什',
        value: 'ALB_LE',
      },
      {
        label: '培拉特',
        value: 'ALB_BR',
      },
      {
        label: '斯库台',
        value: 'ALB_SH',
      },
    ],
  },
  {
    label: '阿尔及利亚',
    value: 'ALG',
  },
  {
    label: '安道尔',
    value: 'AND',
    children: [
      {
        label: '卡尼略',
        value: 'AND_2',
      },
      {
        label: '恩坎普',
        value: 'AND_3',
      },
      {
        label: '马萨纳',
        value: 'AND_4',
      },
      {
        label: '奥尔迪诺',
        value: 'AND_5',
      },
      {
        label: '圣胡利娅－德洛里亚',
        value: 'AND_6',
      },
      {
        label: '安道尔城',
        value: 'AND_7',
      },
      {
        label: '莱塞斯卡尔德－恩戈尔达',
        value: 'AND_8',
      },
    ],
  },
  {
    label: '安哥拉',
    value: 'ANG',
  },
  {
    label: '荷属安地列斯',
    value: 'AHO',
  },
  {
    label: '安提瓜和巴布达',
    value: 'ANT',
  },
  {
    label: '阿拉伯联合酋长国',
    value: 'ARE',
    children: [
      {
        label: '阿布扎比',
        value: 'ARE_AZ',
      },
      {
        label: '艾因',
        value: 'ARE_AL',
      },
      {
        label: '迪拜',
        value: 'ARE_DU',
      },
      {
        label: '沙迦',
        value: 'ARE_SH',
      },
    ],
  },
  {
    label: '阿根廷',
    value: 'ARG',
    children: [
      {
        label: '巴拉那',
        value: 'ARG_PRA',
      },
      {
        label: '别德马',
        value: 'ARG_VDM',
      },
      {
        label: '波萨达斯',
        value: 'ARG_PSS',
      },
      {
        label: '布兰卡港',
        value: 'ARG_BHI',
      },
      {
        label: '布宜诺斯艾利斯',
        value: 'ARG_BUE',
      },
      {
        label: '福莫萨',
        value: 'ARG_FMA',
      },
      {
        label: '胡胡伊',
        value: 'ARG_JUJ',
      },
      {
        label: '卡塔马卡',
        value: 'ARG_CTC',
      },
      {
        label: '科尔多瓦',
        value: 'ARG_COR',
      },
      {
        label: '科连特斯',
        value: 'ARG_CNQ',
      },
      {
        label: '克劳斯城',
        value: 'ARG_VLK',
      },
      {
        label: '肯考迪娅',
        value: 'ARG_COC',
      },
      {
        label: '拉里奥哈',
        value: 'ARG_IRJ',
      },
      {
        label: '拉普拉塔',
        value: 'ARG_LPG',
      },
      {
        label: '雷西斯滕匹亚',
        value: 'ARG_RES',
      },
      {
        label: '里奥加耶戈斯',
        value: 'ARG_RGL',
      },
      {
        label: '里奥夸尔托',
        value: 'ARG_RCU',
      },
      {
        label: '里瓦达维亚海军准将城',
        value: 'ARG_CRD',
      },
      {
        label: '罗萨里奥',
        value: 'ARG_ROS',
      },
      {
        label: '罗森',
        value: 'ARG_RWO',
      },
      {
        label: '马德普拉塔',
        value: 'ARG_MDQ',
      },
      {
        label: '门多萨',
        value: 'ARG_MDZ',
      },
      {
        label: '内乌肯',
        value: 'ARG_NQN',
      },
      {
        label: '萨尔塔',
        value: 'ARG_SLA',
      },
      {
        label: '圣地亚哥-德尔埃斯特罗',
        value: 'ARG_SDE',
      },
      {
        label: '圣菲',
        value: 'ARG_SFN',
      },
      {
        label: '圣胡安',
        value: 'ARG_UAQ',
      },
      {
        label: '圣拉斐尔',
        value: 'ARG_AFA',
      },
      {
        label: '圣路易斯',
        value: 'ARG_LUQ',
      },
      {
        label: '圣罗莎',
        value: 'ARG_RSA',
      },
      {
        label: '圣米格尔-德图库曼',
        value: 'ARG_SMC',
      },
      {
        label: '圣尼古拉斯',
        value: 'ARG_SNS',
      },
      {
        label: '特雷利乌',
        value: 'ARG_REL',
      },
      {
        label: '乌斯怀亚',
        value: 'ARG_USH',
      },
    ],
  },
  {
    label: '亚美尼亚',
    value: 'ARM',
    children: [
      {
        label: '阿尔马维尔',
        value: 'ARM_ARM',
      },
      {
        label: '阿拉加措特恩',
        value: 'ARM_AGT',
      },
      {
        label: '阿拉拉特',
        value: 'ARM_ARA',
      },
      {
        label: '埃里温市',
        value: 'ARM_EVN',
      },
      {
        label: '格加尔库尼克',
        value: 'ARM_GEG',
      },
      {
        label: '科泰克',
        value: 'ARM_KOT',
      },
      {
        label: '洛里',
        value: 'ARM_LOR',
      },
      {
        label: '塔武什',
        value: 'ARM_TAV',
      },
      {
        label: '瓦约茨·佐尔',
        value: 'ARM_VAY',
      },
      {
        label: '希拉克',
        value: 'ARM_SHI',
      },
      {
        label: '休尼克',
        value: 'ARM_SYU',
      },
    ],
  },
  {
    label: '阿森松岛',
    value: 'ASC',
  },
  {
    label: '美属萨摩亚',
    value: 'ASM',
    children: [
      {
        label: '阿纳',
        value: 'ASM_AAN',
      },
      {
        label: '阿图阿',
        value: 'ASM_ATU',
      },
      {
        label: '艾加伊勒泰',
        value: 'ASM_AIT',
      },
      {
        label: '法塞莱莱阿加',
        value: 'ASM_FAA',
      },
      {
        label: '加盖福毛加',
        value: 'ASM_GFG',
      },
      {
        label: '加加埃毛加',
        value: 'ASM_GMG',
      },
      {
        label: '帕劳利',
        value: 'ASM_PAL',
      },
      {
        label: '萨图帕伊泰阿',
        value: 'ASM_SAT',
      },
      {
        label: '萨瓦伊岛',
        value: 'ASM_SAV',
      },
      {
        label: '图阿马萨加',
        value: 'ASM_TUA',
      },
      {
        label: '瓦奥福诺蒂',
        value: 'ASM_VAF',
      },
      {
        label: '韦西加诺',
        value: 'ASM_VAI',
      },
      {
        label: '乌波卢岛',
        value: 'ASM_UPO',
      },
    ],
  },
  {
    label: '南极洲',
    value: 'ATA',
  },
  {
    label: '法属南部领地',
    value: 'ATF',
  },
  {
    label: '安提瓜岛和巴布达',
    value: 'ATG',
  },
  {
    label: '澳大利亚',
    value: 'AUS',
    children: [
      {
        label: '北部地区',
        value: 'AUS_NT',
      },
      {
        label: '堪培拉',
        value: 'AUS_ACT',
      },
      {
        label: '昆士兰',
        value: 'AUS_QLD',
      },
      {
        label: '南澳大利亚',
        value: 'AUS_SA',
      },
      {
        label: '塔斯马尼亚',
        value: 'AUS_TAS',
      },
      {
        label: '维多利亚',
        value: 'AUS_VIC',
      },
      {
        label: '西澳大利亚',
        value: 'AUS_AWA',
      },
      {
        label: '新南威尔士',
        value: 'AUS_NSW',
      },
    ],
  },
  {
    label: '奥地利',
    value: 'AUT',
    children: [
      {
        label: '布尔根兰',
        value: 'AUT_BUR',
      },
      {
        label: '蒂罗尔',
        value: 'AUT_TYR',
      },
      {
        label: '福拉尔贝格',
        value: 'AUT_VOR',
      },
      {
        label: '克恩顿',
        value: 'AUT_CAT',
      },
      {
        label: '萨尔茨堡',
        value: 'AUT_SZG',
      },
      {
        label: '上奥地利',
        value: 'AUT_UAU',
      },
      {
        label: '施蒂利亚',
        value: 'AUT_STY',
      },
      {
        label: '维也纳',
        value: 'AUT_VDD',
      },
      {
        label: '下奥地利',
        value: 'AUT_LAU',
      },
    ],
  },
  {
    label: '阿塞拜疆',
    value: 'AZE',
    children: [
      {
        label: '阿布歇隆',
        value: 'AZE_ABS',
      },
      {
        label: '哈奇马斯',
        value: 'AZE_XAC',
      },
      {
        label: '卡尔巴卡尔',
        value: 'AZE_KAL',
      },
      {
        label: '卡扎赫',
        value: 'AZE_QAZ',
      },
      {
        label: '连科兰',
        value: 'AZE_LAN',
      },
      {
        label: '密尔-卡拉巴赫',
        value: 'AZE_MQA',
      },
      {
        label: '穆甘-萨连',
        value: 'AZE_MSA',
      },
      {
        label: '纳戈尔诺－卡拉巴赫',
        value: 'AZE_NQA',
      },
      {
        label: '纳希切万',
        value: 'AZE_NX',
      },
      {
        label: '普利亚拉克斯',
        value: 'AZE_PRI',
      },
      {
        label: '舍基',
        value: 'AZE_SA',
      },
      {
        label: '苏姆盖特',
        value: 'AZE_SMC',
      },
      {
        label: '锡尔万',
        value: 'AZE_SIR',
      },
      {
        label: '占贾',
        value: 'AZE_GA',
      },
    ],
  },
  {
    label: '巴哈马',
    value: 'BAH',
  },
  {
    label: '孟加拉国',
    value: 'BAN',
  },
  {
    label: '巴巴多斯',
    value: 'BAR',
  },
  {
    label: '布隆迪',
    value: 'BDI',
    children: [
      {
        label: '布班扎',
        value: 'BDI_BB',
      },
      {
        label: '布鲁里',
        value: 'BDI_BR',
      },
      {
        label: '布琼布拉城市',
        value: 'BDI_BM',
      },
      {
        label: '布琼布拉乡村',
        value: 'BDI_BU',
      },
      {
        label: '恩戈齐',
        value: 'BDI_NG',
      },
      {
        label: '基龙多',
        value: 'BDI_KI',
      },
      {
        label: '基特加',
        value: 'BDI_GI',
      },
      {
        label: '卡鲁济',
        value: 'BDI_KR',
      },
      {
        label: '卡扬扎',
        value: 'BDI_KY',
      },
      {
        label: '坎库佐',
        value: 'BDI_CA',
      },
      {
        label: '鲁塔纳',
        value: 'BDI_RT',
      },
      {
        label: '鲁伊吉',
        value: 'BDI_RY',
      },
      {
        label: '马坎巴',
        value: 'BDI_MA',
      },
      {
        label: '穆拉姆维亚',
        value: 'BDI_MU',
      },
      {
        label: '穆瓦洛',
        value: 'BDI_MW',
      },
      {
        label: '穆因加',
        value: 'BDI_MY',
      },
      {
        label: '锡比托凯',
        value: 'BDI_CI',
      },
    ],
  },
  {
    label: '比利时',
    value: 'BEL',
    children: [
      {
        label: '埃诺',
        value: 'BEL_WHT',
      },
      {
        label: '安特卫普',
        value: 'BEL_VAN',
      },
      {
        label: '布拉班特-瓦隆',
        value: 'BEL_WBR',
      },
      {
        label: '布鲁塞尔',
        value: 'BEL_BRU',
      },
      {
        label: '东佛兰德',
        value: 'BEL_VOV',
      },
      {
        label: '佛兰芒-布拉班特',
        value: 'BEL_VBR',
      },
      {
        label: '列日',
        value: 'BEL_WLG',
      },
      {
        label: '林堡',
        value: 'BEL_VLI',
      },
      {
        label: '卢森堡',
        value: 'BEL_WLX',
      },
      {
        label: '那慕尔',
        value: 'BEL_WNA',
      },
      {
        label: '西佛兰德',
        value: 'BEL_VWV',
      },
    ],
  },
  {
    label: '贝宁',
    value: 'BEN',
    children: [
      {
        label: '阿黎博里',
        value: 'BEN_AL',
      },
      {
        label: '阿塔科拉',
        value: 'BEN_AK',
      },
      {
        label: '滨海',
        value: 'BEN_LI',
      },
      {
        label: '波希康市',
        value: 'BEN_BOH',
      },
      {
        label: '博尔古',
        value: 'BEN_BO',
      },
      {
        label: '大西洋',
        value: 'BEN_AQ',
      },
      {
        label: '高原',
        value: 'BEN_PL',
      },
      {
        label: '库福',
        value: 'BEN_KO',
      },
      {
        label: '莫诺',
        value: 'BEN_MO',
      },
      {
        label: '丘陵',
        value: 'BEN_CO',
      },
      {
        label: '韦梅',
        value: 'BEN_OU',
      },
      {
        label: '峡谷',
        value: 'BEN_DO',
      },
      {
        label: '祖',
        value: 'BEN_ZO',
      },
    ],
  },
  {
    label: '布基纳法索',
    value: 'BFA',
    children: [
      {
        label: '巴雷',
        value: 'BFA_BAL',
      },
      {
        label: '巴姆',
        value: 'BFA_BAM',
      },
      {
        label: '巴瓦',
        value: 'BFA_BAN',
      },
      {
        label: '巴泽加',
        value: 'BFA_BAZ',
      },
      {
        label: '波尼',
        value: 'BFA_PON',
      },
      {
        label: '布尔古',
        value: 'BFA_BLG',
      },
      {
        label: '布尔基恩德',
        value: 'BFA_BOK',
      },
      {
        label: '布古里巴',
        value: 'BFA_BOR',
      },
      {
        label: '冈祖尔古',
        value: 'BFA_GAN',
      },
      {
        label: '古尔马',
        value: 'BFA_GOU',
      },
      {
        label: '济罗',
        value: 'BFA_ZIR',
      },
      {
        label: '卡焦戈',
        value: 'BFA_KAD',
      },
      {
        label: '科蒙加里',
        value: 'BFA_KOO',
      },
      {
        label: '科莫埃',
        value: 'BFA_COM',
      },
      {
        label: '孔皮恩加',
        value: 'BFA_KOP',
      },
      {
        label: '孔西',
        value: 'BFA_KOS',
      },
      {
        label: '库尔佩罗戈',
        value: 'BFA_KOL',
      },
      {
        label: '库尔维奥戈',
        value: 'BFA_KOW',
      },
      {
        label: '库里滕加',
        value: 'BFA_KOT',
      },
      {
        label: '雷拉巴',
        value: 'BFA_LER',
      },
      {
        label: '罗卢姆',
        value: 'BFA_LOR',
      },
      {
        label: '穆翁',
        value: 'BFA_MOU',
      },
      {
        label: '纳门滕加',
        value: 'BFA_NAM',
      },
      {
        label: '纳乌里',
        value: 'BFA_NAH',
      },
      {
        label: '纳亚拉',
        value: 'BFA_NAY',
      },
      {
        label: '尼亚尼亚',
        value: 'BFA_GNA',
      },
      {
        label: '努姆比埃尔',
        value: 'BFA_NOU',
      },
      {
        label: '帕索雷',
        value: 'BFA_PAS',
      },
      {
        label: '塞诺',
        value: 'BFA_SEN',
      },
      {
        label: '桑吉',
        value: 'BFA_SAG',
      },
      {
        label: '桑马滕加',
        value: 'BFA_SAM',
      },
      {
        label: '苏鲁',
        value: 'BFA_SOR',
      },
      {
        label: '苏姆',
        value: 'BFA_SOM',
      },
      {
        label: '塔波阿',
        value: 'BFA_TAP',
      },
      {
        label: '图伊',
        value: 'BFA_TUY',
      },
      {
        label: '乌埃',
        value: 'BFA_HOU',
      },
      {
        label: '乌布里滕加',
        value: 'BFA_OUB',
      },
      {
        label: '乌达兰',
        value: 'BFA_OUD',
      },
      {
        label: '锡西里',
        value: 'BFA_SIS',
      },
      {
        label: '亚加',
        value: 'BFA_YAG',
      },
      {
        label: '亚滕加',
        value: 'BFA_YAT',
      },
      {
        label: '伊奥巴',
        value: 'BFA_IOA',
      },
      {
        label: '宗德韦奥戈',
        value: 'BFA_ZOW',
      },
      {
        label: '宗多马',
        value: 'BFA_ZOD',
      },
    ],
  },
  {
    label: '孟加拉',
    value: 'BGD',
    children: [
      {
        label: '达卡',
        value: 'BGD_DAC',
      },
      {
        label: '吉大港',
        value: 'BGD_CGP',
      },
      {
        label: '库尔纳',
        value: 'BGD_KHL',
      },
    ],
  },
  {
    label: '不丹',
    value: 'BHU',
  },
  {
    label: '波斯尼亚和黑塞哥维那',
    value: 'BIH',
    children: [
      {
        label: '波萨维纳',
        value: 'BIH_FPO',
      },
      {
        label: '波斯尼亚－波德里涅',
        value: 'BIH_FBP',
      },
      {
        label: '多米斯拉夫格勒',
        value: 'BIH_FTO',
      },
      {
        label: '黑塞哥维那－涅雷特瓦',
        value: 'BIH_FHN',
      },
      {
        label: '萨拉热窝',
        value: 'BIH_FSA',
      },
      {
        label: '图兹拉－波德里涅',
        value: 'BIH_FTU',
      },
      {
        label: '乌纳－萨纳',
        value: 'BIH_FUS',
      },
      {
        label: '西波斯尼亚',
        value: 'BIH_FHB',
      },
      {
        label: '西黑塞哥维那',
        value: 'BIH_FZH',
      },
      {
        label: '泽尼察－多博伊',
        value: 'BIH_FZE',
      },
      {
        label: '中波斯尼亚',
        value: 'BIH_FSB',
      },
    ],
  },
  {
    label: '伯利兹',
    value: 'BIZ',
  },
  {
    label: '白俄罗斯',
    value: 'BLR',
    children: [
      {
        label: '布列斯特',
        value: 'BLR_BR',
      },
      {
        label: '戈梅利',
        value: 'BLR_HO',
      },
      {
        label: '格罗德诺',
        value: 'BLR_HR',
      },
      {
        label: '明斯克市',
        value: 'BLR_MI',
      },
      {
        label: '莫吉廖夫',
        value: 'BLR_MA',
      },
      {
        label: '维捷布斯克',
        value: 'BLR_VI',
      },
    ],
  },
  {
    label: '百慕大',
    value: 'BMU',
  },
  {
    label: '玻利维亚',
    value: 'BOL',
    children: [
      {
        label: '奥尔托',
        value: 'BOL_ALT',
      },
      {
        label: '奥鲁罗',
        value: 'BOL_ORU',
      },
      {
        label: '贝尼',
        value: 'BOL_BEN',
      },
      {
        label: '波多西',
        value: 'BOL_POI',
      },
      {
        label: '基拉科洛',
        value: 'BOL_QUI',
      },
      {
        label: '科恰班巴',
        value: 'BOL_CBB',
      },
      {
        label: '拉巴斯',
        value: 'BOL_LPB',
      },
      {
        label: '潘多',
        value: 'BOL_PAN',
      },
      {
        label: '丘基萨卡',
        value: 'BOL_CHU',
      },
      {
        label: '萨卡巴',
        value: 'BOL_SAC',
      },
      {
        label: '圣克鲁斯',
        value: 'BOL_SRZ',
      },
      {
        label: '塔里哈',
        value: 'BOL_TJA',
      },
    ],
  },
  {
    label: '博茨瓦纳',
    value: 'BOT',
  },
  {
    label: '巴西',
    value: 'BRA',
    children: [
      {
        label: '阿克里',
        value: 'BRA_AC',
      },
      {
        label: '阿拉戈斯',
        value: 'BRA_AL',
      },
      {
        label: '阿马帕',
        value: 'BRA_AP',
      },
      {
        label: '巴拉那',
        value: 'BRA_PR',
      },
      {
        label: '巴西利亚',
        value: 'BRA_BSB',
      },
      {
        label: '巴伊亚',
        value: 'BRA_BA',
      },
      {
        label: '北里奥格兰德',
        value: 'BRA_RN',
      },
      {
        label: '伯南布哥',
        value: 'BRA_PE',
      },
      {
        label: '戈亚斯',
        value: 'BRA_GO',
      },
      {
        label: '朗多尼亚',
        value: 'BRA_RO',
      },
      {
        label: '里约热内卢',
        value: 'BRA_RJ',
      },
      {
        label: '罗赖马',
        value: 'BRA_RR',
      },
      {
        label: '马拉尼昂',
        value: 'BRA_MA',
      },
      {
        label: '马托格罗索',
        value: 'BRA_MT',
      },
      {
        label: '米纳斯吉拉斯',
        value: 'BRA_MG',
      },
      {
        label: '南里奥格兰德',
        value: 'BRA_RS',
      },
      {
        label: '南马托格罗索',
        value: 'BRA_MS',
      },
      {
        label: '帕拉',
        value: 'BRA_PA',
      },
      {
        label: '帕拉伊巴',
        value: 'BRA_PB',
      },
      {
        label: '皮奥伊',
        value: 'BRA_PI',
      },
      {
        label: '塞阿拉',
        value: 'BRA_CE',
      },
      {
        label: '塞尔希培',
        value: 'BRA_SE',
      },
      {
        label: '圣埃斯皮里图',
        value: 'BRA_ES',
      },
      {
        label: '圣保罗',
        value: 'BRA_SP',
      },
      {
        label: '圣卡塔琳娜',
        value: 'BRA_SC',
      },
      {
        label: '托坎廷斯',
        value: 'BRA_TO',
      },
      {
        label: '亚马孙',
        value: 'BRA_AM',
      },
    ],
  },
  {
    label: '巴巴多斯岛',
    value: 'BRB',
  },
  {
    label: '巴林',
    value: 'BRN',
  },
  {
    label: '文莱',
    value: 'BRU',
  },
  {
    label: '保加利亚',
    value: 'BUL',
  },
  {
    label: '布基纳国',
    value: 'BUR',
  },
  {
    label: '布韦岛',
    value: 'BVT',
  },
  {
    label: '中非共和国',
    value: 'CAF',
    children: [
      {
        label: '巴明吉-班戈兰',
        value: 'CAF_BB',
      },
      {
        label: '班吉直辖市',
        value: 'CAF_BGF',
      },
      {
        label: '宾博',
        value: 'CAF_BI',
      },
      {
        label: '凯莫',
        value: 'CAF_KG',
      },
      {
        label: '洛巴伊',
        value: 'CAF_LB',
      },
      {
        label: '曼贝雷-卡代',
        value: 'CAF_HS',
      },
      {
        label: '姆博穆',
        value: 'CAF_MB',
      },
      {
        label: '纳纳-格里比齐',
        value: 'CAF_KB',
      },
      {
        label: '纳纳-曼贝雷',
        value: 'CAF_NM',
      },
      {
        label: '桑加-姆巴埃雷',
        value: 'CAF_SE',
      },
      {
        label: '上科托',
        value: 'CAF_HK',
      },
      {
        label: '上姆博穆',
        value: 'CAF_HM',
      },
      {
        label: '瓦卡',
        value: 'CAF_UK',
      },
      {
        label: '瓦卡加',
        value: 'CAF_VK',
      },
      {
        label: '瓦姆',
        value: 'CAF_AC',
      },
      {
        label: '瓦姆-彭代',
        value: 'CAF_OP',
      },
      {
        label: '翁贝拉-姆波科',
        value: 'CAF_MP',
      },
      {
        label: '下科托',
        value: 'CAF_BK',
      },
    ],
  },
  {
    label: '柬埔寨',
    value: 'CAM',
  },
  {
    label: '加拿大',
    value: 'CAN',
    children: [
      {
        label: '阿伯茨福',
        value: 'CAN_ABB',
      },
      {
        label: '埃德蒙顿',
        value: 'CAN_EDM',
      },
      {
        label: '奥沙瓦',
        value: 'CAN_OSH',
      },
      {
        label: '巴里',
        value: 'CAN_BAR',
      },
      {
        label: '布列塔尼角',
        value: 'CAN_CBR',
      },
      {
        label: '多伦多',
        value: 'CAN_TOR',
      },
      {
        label: '弗雷德里顿',
        value: 'CAN_FRE',
      },
      {
        label: '圭尔夫',
        value: 'CAN_GLP',
      },
      {
        label: '哈利法克斯',
        value: 'CAN_HAL',
      },
      {
        label: '哈密尔顿',
        value: 'CAN_HAM',
      },
      {
        label: '怀特霍斯',
        value: 'CAN_YXY',
      },
      {
        label: '基劳纳',
        value: 'CAN_KWL',
      },
      {
        label: '基奇纳',
        value: 'CAN_BRP',
      },
      {
        label: '金斯敦',
        value: 'CAN_KGN',
      },
      {
        label: '卡里加里',
        value: 'CAN_CAL',
      },
      {
        label: '魁北克',
        value: 'CAN_QUE',
      },
      {
        label: '里贾纳',
        value: 'CAN_REG',
      },
      {
        label: '伦敦',
        value: 'CAN_LOD',
      },
      {
        label: '蒙特利尔',
        value: 'CAN_MTR',
      },
      {
        label: '萨德伯里',
        value: 'CAN_SUD',
      },
      {
        label: '萨斯卡通',
        value: 'CAN_SAK',
      },
      {
        label: '三河城',
        value: 'CAN_TRR',
      },
      {
        label: '桑德贝',
        value: 'CAN_THU',
      },
      {
        label: '舍布鲁克',
        value: 'CAN_SBE',
      },
      {
        label: '圣卡塔琳娜',
        value: 'CAN_SCA',
      },
      {
        label: '圣约翰斯',
        value: 'CAN_SJB',
      },
      {
        label: '维多利亚',
        value: 'CAN_VIC',
      },
      {
        label: '温哥华',
        value: 'CAN_VAN',
      },
      {
        label: '温尼伯',
        value: 'CAN_WNP',
      },
      {
        label: '温莎',
        value: 'CAN_WDR',
      },
      {
        label: '渥太华',
        value: 'CAN_OTT',
      },
      {
        label: '夏洛特敦',
        value: 'CAN_CHA',
      },
      {
        label: '耶洛奈夫',
        value: 'CAN_YZF',
      },
      {
        label: '伊魁特',
        value: 'CAN_IQL',
      },
    ],
  },
  {
    label: '科科斯群岛',
    value: 'CCK',
  },
  {
    label: '刚果',
    value: 'CGO',
  },
  {
    label: '乍得',
    value: 'CHA',
  },
  {
    label: '智利',
    value: 'CHI',
  },
  {
    label: '科特迪瓦',
    value: 'CIV',
    children: [
      {
        label: '阿涅比',
        value: 'CIV_AG',
      },
      {
        label: '巴芬',
        value: 'CIV_BF',
      },
      {
        label: '邦达马河谷',
        value: 'CIV_VB',
      },
      {
        label: '登盖莱',
        value: 'CIV_DE',
      },
      {
        label: '恩济－科莫埃',
        value: 'CIV_NC',
      },
      {
        label: '弗罗马格尔',
        value: 'CIV_FR',
      },
      {
        label: '湖泊',
        value: 'CIV_LC',
      },
      {
        label: '马拉韦',
        value: 'CIV_MR',
      },
      {
        label: '南科莫埃',
        value: 'CIV_SC',
      },
      {
        label: '萨桑德拉',
        value: 'CIV_HT',
      },
      {
        label: '萨瓦纳',
        value: 'CIV_SV',
      },
      {
        label: '山地',
        value: 'CIV_DH',
      },
      {
        label: '沃罗杜古',
        value: 'CIV_WR',
      },
      {
        label: '下萨桑德拉',
        value: 'CIV_BS',
      },
      {
        label: '泻湖',
        value: 'CIV_LG',
      },
      {
        label: '赞赞',
        value: 'CIV_ZA',
      },
      {
        label: '中卡瓦利',
        value: 'CIV_MV',
      },
      {
        label: '中科莫埃',
        value: 'CIV_MC',
      },
    ],
  },
  {
    label: '喀麦隆',
    value: 'CMR',
    children: [
      {
        label: '阿达马瓦',
        value: 'CMR_ADA',
      },
      {
        label: '北部',
        value: 'CMR_NOR',
      },
      {
        label: '北端',
        value: 'CMR_EXN',
      },
      {
        label: '滨海',
        value: 'CMR_LIT',
      },
      {
        label: '东部',
        value: 'CMR_EST',
      },
      {
        label: '南部',
        value: 'CMR_SUD',
      },
      {
        label: '西北',
        value: 'CMR_NOT',
      },
      {
        label: '西部',
        value: 'CMR_OUE',
      },
      {
        label: '西南',
        value: 'CMR_SOU',
      },
      {
        label: '中央',
        value: 'CMR_CEN',
      },
    ],
  },
  {
    label: '刚果民主共和国',
    value: 'COD',
  },
  {
    label: '库克群岛',
    value: 'COK',
  },
  {
    label: '哥伦比亚',
    value: 'COL',
    children: [
      {
        label: '阿劳卡',
        value: 'COL_ARA',
      },
      {
        label: '安提奥基亚',
        value: 'COL_ANT',
      },
      {
        label: '北桑坦德',
        value: 'COL_NDS',
      },
      {
        label: '波哥大首都区',
        value: 'COL_BDC',
      },
      {
        label: '博利瓦尔',
        value: 'COL_BOL',
      },
      {
        label: '博亚卡',
        value: 'COL_BOY',
      },
      {
        label: '大西洋',
        value: 'COL_ATL',
      },
      {
        label: '瓜维亚雷',
        value: 'COL_GVR',
      },
      {
        label: '瓜希拉',
        value: 'COL_GJR',
      },
      {
        label: '瓜伊尼亚',
        value: 'COL_GNA',
      },
      {
        label: '金迪奥',
        value: 'COL_QUI',
      },
      {
        label: '卡尔达斯',
        value: 'COL_CAL',
      },
      {
        label: '卡克塔',
        value: 'COL_CAQ',
      },
      {
        label: '卡萨纳雷',
        value: 'COL_CAS',
      },
      {
        label: '考卡',
        value: 'COL_CAU',
      },
      {
        label: '考卡山谷',
        value: 'COL_VDC',
      },
      {
        label: '科尔多巴',
        value: 'COL_COR',
      },
      {
        label: '昆迪纳马卡',
        value: 'COL_CAM',
      },
      {
        label: '利萨拉尔达',
        value: 'COL_RIS',
      },
      {
        label: '马格达雷那',
        value: 'COL_MAG',
      },
      {
        label: '梅塔',
        value: 'COL_MET',
      },
      {
        label: '纳里尼奥',
        value: 'COL_NAR',
      },
      {
        label: '普图马约',
        value: 'COL_PUT',
      },
      {
        label: '乔科',
        value: 'COL_CHO',
      },
      {
        label: '塞萨尔',
        value: 'COL_CES',
      },
      {
        label: '桑坦德',
        value: 'COL_SAN',
      },
      {
        label: '圣安德烈斯-普罗维登西亚',
        value: 'COL_SAP',
      },
      {
        label: '苏克雷',
        value: 'COL_SUC',
      },
      {
        label: '托利马',
        value: 'COL_TOL',
      },
      {
        label: '维查达',
        value: 'COL_VIC',
      },
      {
        label: '沃佩斯',
        value: 'COL_VAU',
      },
      {
        label: '乌伊拉',
        value: 'COL_HUI',
      },
      {
        label: '亚马孙',
        value: 'COL_AMZ',
      },
    ],
  },
  {
    label: '科摩罗',
    value: 'COM',
  },
  {
    label: '佛得角',
    value: 'CPV',
    children: [
      {
        label: '保尔',
        value: 'CPV_PA',
      },
      {
        label: '波多诺伏',
        value: 'CPV_PN',
      },
      {
        label: '博阿维斯塔岛',
        value: 'CPV_BV',
      },
      {
        label: '布拉瓦岛',
        value: 'CPV_BR',
      },
      {
        label: '大里贝拉',
        value: 'CPV_RG',
      },
      {
        label: '福古岛',
        value: 'CPV_FO',
      },
      {
        label: '马尤岛',
        value: 'CPV_MA',
      },
      {
        label: '莫斯特罗',
        value: 'CPV_MO',
      },
      {
        label: '普拉亚',
        value: 'CPV_PR',
      },
      {
        label: '萨尔岛',
        value: 'CPV_SL',
      },
      {
        label: '圣安唐岛',
        value: 'CPV_SA',
      },
      {
        label: '圣地亚哥岛',
        value: 'CPV_IA',
      },
      {
        label: '圣多明戈',
        value: 'CPV_SD',
      },
      {
        label: '圣菲利普',
        value: 'CPV_SF',
      },
      {
        label: '圣卡塔琳娜',
        value: 'CPV_CA',
      },
      {
        label: '圣克鲁斯',
        value: 'CPV_CR',
      },
      {
        label: '圣米戈尔',
        value: 'CPV_SM',
      },
      {
        label: '圣尼古拉岛',
        value: 'CPV_SN',
      },
      {
        label: '圣维森特岛',
        value: 'CPV_SV',
      },
      {
        label: '塔拉法尔',
        value: 'CPV_TA',
      },
    ],
  },
  {
    label: '哥斯达黎加',
    value: 'CRC',
  },
  {
    label: '克罗地亚',
    value: 'CRO',
  },
  {
    label: '古巴',
    value: 'CUB',
    children: [
      {
        label: '比那尔德里奥',
        value: 'CUB_1',
      },
      {
        label: '哈瓦那',
        value: 'CUB_2',
      },
      {
        label: '哈瓦那城',
        value: 'CUB_3',
      },
      {
        label: '马坦萨斯',
        value: 'CUB_4',
      },
      {
        label: '比亚克拉拉',
        value: 'CUB_5',
      },
      {
        label: '西恩富戈斯',
        value: 'CUB_6',
      },
      {
        label: '圣斯皮里图斯',
        value: 'CUB_7',
      },
      {
        label: '谢戈德阿维拉',
        value: 'CUB_8',
      },
      {
        label: '卡马圭',
        value: 'CUB_9',
      },
      {
        label: '拉斯图纳斯',
        value: 'CUB_10',
      },
      {
        label: '奥尔金',
        value: 'CUB_11',
      },
      {
        label: '格拉玛',
        value: 'CUB_12',
      },
      {
        label: '圣地亚哥',
        value: 'CUB_13',
      },
      {
        label: '关塔那摩',
        value: 'CUB_14',
      },
      {
        label: '青年岛特区',
        value: 'CUB_99',
      },
      {
        label: '马亚里',
        value: 'CUB_MAY',
      },
      {
        label: '曼萨尼罗',
        value: 'CUB_MZO',
      },
    ],
  },
  {
    label: '圣诞岛',
    value: 'CXR',
  },
  {
    label: '开曼群岛',
    value: 'CYM',
  },
  {
    label: '塞浦路斯',
    value: 'CYP',
    children: [
      {
        label: '尼科西亚',
        value: 'CYP_1',
      },
      {
        label: '利马索尔',
        value: 'CYP_2',
      },
      {
        label: '拉纳卡',
        value: 'CYP_3',
      },
      {
        label: '法马古斯塔',
        value: 'CYP_4',
      },
      {
        label: '帕福斯',
        value: 'CYP_5',
      },
      {
        label: '凯里尼亚',
        value: 'CYP_6',
      },
    ],
  },
  {
    label: '捷克共和国',
    value: 'CZE',
    children: [
      {
        label: '奥洛穆茨',
        value: 'CZE_OL',
      },
      {
        label: '比尔森',
        value: 'CZE_PL',
      },
      {
        label: '布拉格直辖市',
        value: 'CZE_PR',
      },
      {
        label: '赫拉德茨-克拉洛韦',
        value: 'CZE_KR',
      },
      {
        label: '卡罗维发利',
        value: 'CZE_KA',
      },
      {
        label: '利贝雷克',
        value: 'CZE_LI',
      },
      {
        label: '摩拉维亚-西里西亚',
        value: 'CZE_MO',
      },
      {
        label: '南摩拉维亚',
        value: 'CZE_JC',
      },
      {
        label: '帕尔杜比采',
        value: 'CZE_PA',
      },
      {
        label: '维索基纳',
        value: 'CZE_VY',
      },
      {
        label: '乌斯季',
        value: 'CZE_US',
      },
      {
        label: '中捷克',
        value: 'CZE_ST',
      },
      {
        label: '兹林',
        value: 'CZE_ZL',
      },
    ],
  },
  {
    label: '丹麦',
    value: 'DEN',
  },
  {
    label: '吉布提',
    value: 'DJI',
    children: [
      {
        label: '阿里萨比赫区',
        value: 'DJI_S',
      },
      {
        label: '奥博克区',
        value: 'DJI_O',
      },
      {
        label: '迪基勒区',
        value: 'DJI_K',
      },
      {
        label: '塔朱拉区',
        value: 'DJI_T',
      },
    ],
  },
  {
    label: '多米尼克',
    value: 'DMA',
  },
  {
    label: '多米尼加共和国',
    value: 'DOM',
  },
  {
    label: '厄瓜多尔',
    value: 'ECU',
    children: [
      {
        label: '阿苏艾',
        value: 'ECU_A',
      },
      {
        label: '埃尔奥罗',
        value: 'ECU_O',
      },
      {
        label: '埃斯梅拉尔达斯',
        value: 'ECU_E',
      },
      {
        label: '玻利瓦尔',
        value: 'ECU_B',
      },
      {
        label: '瓜亚斯',
        value: 'ECU_G',
      },
      {
        label: '加拉帕戈斯',
        value: 'ECU_W',
      },
      {
        label: '卡尔奇',
        value: 'ECU_C',
      },
      {
        label: '卡尼亚尔',
        value: 'ECU_F',
      },
      {
        label: '科托帕希',
        value: 'ECU_X',
      },
      {
        label: '洛哈',
        value: 'ECU_L',
      },
      {
        label: '洛斯里奥斯',
        value: 'ECU_R',
      },
      {
        label: '马纳比',
        value: 'ECU_M',
      },
      {
        label: '莫罗纳－圣地亚哥',
        value: 'ECU_S',
      },
      {
        label: '纳波，奥雷利亚纳',
        value: 'ECU_D',
      },
      {
        label: '帕斯塔萨',
        value: 'ECU_Y',
      },
      {
        label: '皮钦查',
        value: 'ECU_P',
      },
      {
        label: '钦博拉索',
        value: 'ECU_H',
      },
      {
        label: '萨莫拉－钦奇佩',
        value: 'ECU_Z',
      },
      {
        label: '苏昆毕奥斯',
        value: 'ECU_U',
      },
      {
        label: '通古拉瓦',
        value: 'ECU_T',
      },
      {
        label: '因巴布拉',
        value: 'ECU_I',
      },
    ],
  },
  {
    label: '埃及',
    value: 'EGY',
    children: [
      {
        label: '阿斯旺',
        value: 'EGY_ASW',
      },
      {
        label: '古尔代盖',
        value: 'EGY_GBY',
      },
      {
        label: '开罗',
        value: 'EGY_CAI',
      },
      {
        label: '苏布拉开马',
        value: 'EGY_SKH',
      },
      {
        label: '亚历山大',
        value: 'EGY_ALY',
      },
    ],
  },
  {
    label: '厄立特里亚',
    value: 'ERI',
    children: [
      {
        label: '安塞巴',
        value: 'ERI_KE',
      },
      {
        label: '北红海',
        value: 'ERI_SK',
      },
      {
        label: '加什·巴尔卡',
        value: 'ERI_BR',
      },
      {
        label: '南部',
        value: 'ERI_DE',
      },
      {
        label: '南红海',
        value: 'ERI_DK',
      },
      {
        label: '中部',
        value: 'ERI_MA',
      },
    ],
  },
  {
    label: '萨尔瓦多',
    value: 'ESA',
  },
  {
    label: '西班牙',
    value: 'ESP',
    children: [
      {
        label: '阿尔梅里亚',
        value: 'ESP_LEI',
      },
      {
        label: '阿尔瓦塞特',
        value: 'ESP_ALB',
      },
      {
        label: '阿拉瓦',
        value: 'ESP_ALA',
      },
      {
        label: '阿利坎特',
        value: 'ESP_ALC',
      },
      {
        label: '阿斯图利亚斯',
        value: 'ESP_AST',
      },
      {
        label: '阿维拉',
        value: 'ESP_AVI',
      },
      {
        label: '奥伦塞',
        value: 'ESP_ORE',
      },
      {
        label: '巴达霍斯',
        value: 'ESP_BJZ',
      },
      {
        label: '巴利阿里',
        value: 'ESP_BLR',
      },
      {
        label: '巴利亚多利德',
        value: 'ESP_VLL',
      },
      {
        label: '巴伦西亚',
        value: 'ESP_VLC',
      },
      {
        label: '巴塞罗那',
        value: 'ESP_BCN',
      },
      {
        label: '比斯开',
        value: 'ESP_VSE',
      },
      {
        label: '布尔戈斯',
        value: 'ESP_BUR',
      },
      {
        label: '格拉纳达',
        value: 'ESP_GRX',
      },
      {
        label: '瓜达拉哈拉',
        value: 'ESP_GUA',
      },
      {
        label: '哈恩',
        value: 'ESP_JAE',
      },
      {
        label: '赫罗纳',
        value: 'ESP_GRO',
      },
      {
        label: '吉普斯夸',
        value: 'ESP_GUI',
      },
      {
        label: '加的斯',
        value: 'ESP_CAD',
      },
      {
        label: '卡塞雷斯',
        value: 'ESP_CCS',
      },
      {
        label: '卡斯蒂利亚',
        value: 'ESP_CIR',
      },
      {
        label: '卡斯特利翁',
        value: 'ESP_CAS',
      },
      {
        label: '科尔多瓦',
        value: 'ESP_ODB',
      },
      {
        label: '昆卡',
        value: 'ESP_CUE',
      },
      {
        label: '拉科鲁尼亚',
        value: 'ESP_LCG',
      },
      {
        label: '拉里奥哈',
        value: 'ESP_ARL',
      },
      {
        label: '拉斯帕尔马斯',
        value: 'ESP_LPA',
      },
      {
        label: '莱昂',
        value: 'ESP_LEN',
      },
      {
        label: '莱里达',
        value: 'ESP_LLE',
      },
      {
        label: '卢戈',
        value: 'ESP_LGO',
      },
      {
        label: '马德里',
        value: 'ESP_MAD',
      },
      {
        label: '马拉加',
        value: 'ESP_AGP',
      },
      {
        label: '穆尔西亚',
        value: 'ESP_MJV',
      },
      {
        label: '纳瓦拉',
        value: 'ESP_NVV',
      },
      {
        label: '帕伦西亚',
        value: 'ESP_PAC',
      },
      {
        label: '蓬特韦德拉',
        value: 'ESP_PEV',
      },
      {
        label: '萨拉戈萨',
        value: 'ESP_ZAZ',
      },
      {
        label: '萨拉曼卡',
        value: 'ESP_SLM',
      },
      {
        label: '萨莫拉',
        value: 'ESP_ZMR',
      },
      {
        label: '塞哥维亚',
        value: 'ESP_SEG',
      },
      {
        label: '塞维利亚',
        value: 'ESP_SVQ',
      },
      {
        label: '桑坦德',
        value: 'ESP_SDR',
      },
      {
        label: '圣克鲁斯-德特内里费',
        value: 'ESP_SCT',
      },
      {
        label: '索里亚',
        value: 'ESP_SOR',
      },
      {
        label: '塔拉戈纳',
        value: 'ESP_TAR',
      },
      {
        label: '特鲁埃尔',
        value: 'ESP_TER',
      },
      {
        label: '托莱多',
        value: 'ESP_TOL',
      },
      {
        label: '韦尔瓦',
        value: 'ESP_HUV',
      },
      {
        label: '韦斯卡',
        value: 'ESP_HUC',
      },
    ],
  },
  {
    label: '爱沙尼亚',
    value: 'EST',
    children: [
      {
        label: '哈留',
        value: 'EST_37',
      },
      {
        label: '希尤',
        value: 'EST_39',
      },
      {
        label: '依达－维鲁',
        value: 'EST_44',
      },
      {
        label: '耶盖瓦',
        value: 'EST_49',
      },
      {
        label: '耶尔韦',
        value: 'EST_51',
      },
      {
        label: '里亚内',
        value: 'EST_57',
      },
      {
        label: '维鲁',
        value: 'EST_59',
      },
      {
        label: '贝尔瓦',
        value: 'EST_65',
      },
      {
        label: '帕尔努',
        value: 'EST_67',
      },
      {
        label: '拉普拉',
        value: 'EST_70',
      },
      {
        label: '萨雷',
        value: 'EST_74',
      },
      {
        label: '塔尔图',
        value: 'EST_78',
      },
      {
        label: '瓦尔加',
        value: 'EST_82',
      },
      {
        label: '维良地',
        value: 'EST_84',
      },
      {
        label: '沃鲁',
        value: 'EST_86',
      },
    ],
  },
  {
    label: '埃塞俄比亚',
    value: 'ETH',
    children: [
      {
        label: '阿法尔',
        value: 'ETH_AF',
      },
      {
        label: '阿姆哈拉',
        value: 'ETH_AH',
      },
      {
        label: '奥罗米亚',
        value: 'ETH_OR',
      },
      {
        label: '宾香古尔',
        value: 'ETH_BG',
      },
      {
        label: '德雷达瓦',
        value: 'ETH_DD',
      },
      {
        label: '甘贝拉各族',
        value: 'ETH_GB',
      },
      {
        label: '哈勒里民族',
        value: 'ETH_HR',
      },
      {
        label: '南方各族',
        value: 'ETH_SN',
      },
      {
        label: '索马里',
        value: 'ETH_SM',
      },
      {
        label: '提格雷',
        value: 'ETH_TG',
      },
      {
        label: '亚的斯亚贝巴',
        value: 'ETH_AA',
      },
    ],
  },
  {
    label: '斐济',
    value: 'FIJ',
  },
  {
    label: '芬兰',
    value: 'FIN',
    children: [
      {
        label: '埃斯波',
        value: 'FIN_ESP',
      },
      {
        label: '奥卢',
        value: 'FIN_OLU',
      },
      {
        label: '波里',
        value: 'FIN_POR',
      },
      {
        label: '博尔沃',
        value: 'FIN_PRV',
      },
      {
        label: '海门林纳',
        value: 'FIN_HMY',
      },
      {
        label: '赫尔辛基',
        value: 'FIN_HEL',
      },
      {
        label: '卡亚尼',
        value: 'FIN_KAJ',
      },
      {
        label: '科科拉',
        value: 'FIN_KOK',
      },
      {
        label: '科特卡',
        value: 'FIN_KTK',
      },
      {
        label: '库奥皮奥',
        value: 'FIN_KUO',
      },
      {
        label: '拉赫蒂',
        value: 'FIN_LHI',
      },
      {
        label: '拉彭兰塔',
        value: 'FIN_LPP',
      },
      {
        label: '罗瓦涅米',
        value: 'FIN_RVN',
      },
      {
        label: '玛丽港',
        value: 'FIN_MHQ',
      },
      {
        label: '米凯利',
        value: 'FIN_MIK',
      },
      {
        label: '坦佩雷',
        value: 'FIN_TMP',
      },
      {
        label: '图尔库',
        value: 'FIN_TKU',
      },
      {
        label: '瓦萨',
        value: 'FIN_VAA',
      },
      {
        label: '万塔',
        value: 'FIN_VAT',
      },
      {
        label: '约恩苏',
        value: 'FIN_JOE',
      },
    ],
  },
  {
    label: '弗兰克群岛',
    value: 'FLK',
  },
  {
    label: '法国',
    value: 'FRA',
    children: [
      {
        label: '阿尔勒',
        value: 'FRA_ARL',
      },
      {
        label: '阿雅克修',
        value: 'FRA_AJA',
      },
      {
        label: '艾克斯',
        value: 'FRA_QXB',
      },
      {
        label: '奥尔良',
        value: 'FRA_ORR',
      },
      {
        label: '巴黎',
        value: 'FRA_PAR',
      },
      {
        label: '贝桑松',
        value: 'FRA_BSN',
      },
      {
        label: '第戎',
        value: 'FRA_DIJ',
      },
      {
        label: '弗雷瑞斯',
        value: 'FRA_FRJ',
      },
      {
        label: '卡昂',
        value: 'FRA_CFR',
      },
      {
        label: '雷恩',
        value: 'FRA_RNS',
      },
      {
        label: '里昂',
        value: 'FRA_LIO',
      },
      {
        label: '里尔',
        value: 'FRA_LLE',
      },
      {
        label: '利摩日',
        value: 'FRA_LIG',
      },
      {
        label: '鲁昂',
        value: 'FRA_URO',
      },
      {
        label: '马赛',
        value: 'FRA_MRS',
      },
      {
        label: '梅斯',
        value: 'FRA_MZM',
      },
      {
        label: '蒙彼利埃',
        value: 'FRA_MPL',
      },
      {
        label: '南特',
        value: 'FRA_NTE',
      },
      {
        label: '尼斯',
        value: 'FRA_NCE',
      },
      {
        label: '沙隆',
        value: 'FRA_CSM',
      },
      {
        label: '图卢兹',
        value: 'FRA_TLS',
      },
      {
        label: '瓦朗斯',
        value: 'FRA_VAA',
      },
      {
        label: '亚眠',
        value: 'FRA_AMI',
      },
    ],
  },
  {
    label: '法罗群岛',
    value: 'FRO',
  },
  {
    label: '密克罗尼西亚',
    value: 'FSM',
  },
  {
    label: '加蓬',
    value: 'GAB',
    children: [
      {
        label: '奥果韦-洛洛',
        value: 'GAB_OL',
      },
      {
        label: '奥果韦-伊温多',
        value: 'GAB_OI',
      },
      {
        label: '滨海奥果韦',
        value: 'GAB_OM',
      },
      {
        label: '恩古涅',
        value: 'GAB_NG',
      },
      {
        label: '河口',
        value: 'GAB_ES',
      },
      {
        label: '尼扬加',
        value: 'GAB_NY',
      },
      {
        label: '上奥果韦',
        value: 'GAB_HO',
      },
      {
        label: '沃勒-恩特姆',
        value: 'GAB_WN',
      },
      {
        label: '中奥果韦',
        value: 'GAB_MO',
      },
    ],
  },
  {
    label: '冈比亚',
    value: 'GAM',
  },
  {
    label: '泽西',
    value: 'GBJ',
  },
  {
    label: '英国',
    value: 'GBR',
    children: [
      {
        label: '北爱尔兰',
        value: 'GBR_NIR',
      },
      {
        label: '苏格兰',
        value: 'GBR_SCT',
      },
      {
        label: '威尔士',
        value: 'GBR_WLS',
      },
      {
        label: '英格兰',
        value: 'GBR_ENG',
      },
    ],
  },
  {
    label: '格鲁吉亚',
    value: 'GEO',
  },
  {
    label: '赤道几内亚',
    value: 'GEQ',
  },
  {
    label: '德国',
    value: 'GER',
  },
  {
    label: '格恩西岛',
    value: 'GGY',
  },
  {
    label: '加纳',
    value: 'GHA',
    children: [
      {
        label: '阿散蒂',
        value: 'GHA_AS',
      },
      {
        label: '奥布阿西',
        value: 'GHA_OBU',
      },
      {
        label: '北部',
        value: 'GHA_NO',
      },
      {
        label: '布朗阿哈福',
        value: 'GHA_BA',
      },
      {
        label: '大阿克拉',
        value: 'GHA_GA',
      },
      {
        label: '东部',
        value: 'GHA_EA',
      },
      {
        label: '上东部',
        value: 'GHA_UE',
      },
      {
        label: '上西部',
        value: 'GHA_UW',
      },
      {
        label: '沃尔特',
        value: 'GHA_VO',
      },
      {
        label: '西部',
        value: 'GHA_WE',
      },
      {
        label: '中部',
        value: 'GHA_CE',
      },
    ],
  },
  {
    label: '直布罗陀',
    value: 'GIB',
  },
  {
    label: '瓜德罗普',
    value: 'GLP',
  },
  {
    label: '几内亚比绍',
    value: 'GNB',
  },
  {
    label: '希腊',
    value: 'GRE',
  },
  {
    label: '格陵兰',
    value: 'GRL',
  },
  {
    label: '格林纳达',
    value: 'GRN',
  },
  {
    label: '危地马拉',
    value: 'GUA',
  },
  {
    label: '法属圭亚那',
    value: 'GUF',
  },
  {
    label: '几内亚',
    value: 'GUI',
  },
  {
    label: '关岛',
    value: 'GUM',
  },
  {
    label: '圭亚那',
    value: 'GUY',
    children: [
      {
        label: '埃塞奎博群岛-西德梅拉拉',
        value: 'GUY_EW',
      },
      {
        label: '巴里马-瓦伊尼',
        value: 'GUY_BW',
      },
      {
        label: '波默伦-苏佩纳姆',
        value: 'GUY_PM',
      },
      {
        label: '波塔罗-锡帕鲁尼',
        value: 'GUY_PI',
      },
      {
        label: '德梅拉拉-马海卡',
        value: 'GUY_DM',
      },
      {
        label: '东伯比斯-科兰太因',
        value: 'GUY_EC',
      },
      {
        label: '库尤尼-马扎鲁尼',
        value: 'GUY_CM',
      },
      {
        label: '马海卡-伯比斯',
        value: 'GUY_MB',
      },
      {
        label: '上德梅拉拉-伯比斯',
        value: 'GUY_UD',
      },
      {
        label: '上塔库图-上埃塞奎博',
        value: 'GUY_UT',
      },
    ],
  },
  {
    label: '海地',
    value: 'HAI',
  },
  {
    label: '赫德和麦克唐纳群岛',
    value: 'HMD',
  },
  {
    label: '洪都拉斯',
    value: 'HON',
  },
  {
    label: '匈牙利',
    value: 'HUN',
    children: [
      {
        label: '巴兰尼亚',
        value: 'HUN_BA',
      },
      {
        label: '巴奇-基什孔',
        value: 'HUN_BK',
      },
      {
        label: '包尔绍德-奥包乌伊-曾普伦',
        value: 'HUN_BZ',
      },
      {
        label: '贝凯什',
        value: 'HUN_BE',
      },
      {
        label: '布达佩斯',
        value: 'HUN_BU',
      },
      {
        label: '费耶尔',
        value: 'HUN_FE',
      },
      {
        label: '豪伊杜-比豪尔',
        value: 'HUN_HB',
      },
      {
        label: '赫维什',
        value: 'HUN_HE',
      },
      {
        label: '加兹-纳杰孔-索尔诺克',
        value: 'HUN_JN',
      },
      {
        label: '杰尔-莫松-肖普朗',
        value: 'HUN_GS',
      },
      {
        label: '科马罗姆',
        value: 'HUN_KE',
      },
      {
        label: '诺格拉德',
        value: 'HUN_NO',
      },
      {
        label: '佩斯',
        value: 'HUN_PE',
      },
      {
        label: '琼格拉德',
        value: 'HUN_CS',
      },
      {
        label: '绍莫吉',
        value: 'HUN_SO',
      },
      {
        label: '索博尔奇-索特马尔-贝拉格',
        value: 'HUN_SZ',
      },
      {
        label: '托尔瑙',
        value: 'HUN_TO',
      },
      {
        label: '维斯普雷姆',
        value: 'HUN_VE',
      },
      {
        label: '沃什',
        value: 'HUN_VA',
      },
      {
        label: '佐洛',
        value: 'HUN_ZA',
      },
    ],
  },
  {
    label: '印度尼西亚',
    value: 'IDN',
    children: [
      {
        label: '巴厘',
        value: 'IDN_BA',
      },
      {
        label: '邦加－勿里洞群岛',
        value: 'IDN_BB',
      },
      {
        label: '北苏拉威西',
        value: 'IDN_SA',
      },
      {
        label: '北苏门答腊',
        value: 'IDN_SU',
      },
      {
        label: '大雅加达首都特区',
        value: 'IDN_KB',
      },
      {
        label: '东加里曼丹',
        value: 'IDN_KI',
      },
      {
        label: '东南苏拉威西',
        value: 'IDN_SG',
      },
      {
        label: '东努沙登加拉',
        value: 'IDN_NT',
      },
      {
        label: '东爪哇',
        value: 'IDN_JI',
      },
      {
        label: '廖内',
        value: 'IDN_RI',
      },
      {
        label: '马鲁古',
        value: 'IDN_MA',
      },
      {
        label: '明古鲁',
        value: 'IDN_BE',
      },
      {
        label: '楠榜',
        value: 'IDN_LA',
      },
      {
        label: '南加里曼丹',
        value: 'IDN_KS',
      },
      {
        label: '南苏拉威西',
        value: 'IDN_SN',
      },
      {
        label: '南苏门答腊',
        value: 'IDN_SS',
      },
      {
        label: '日惹特区',
        value: 'IDN_YO',
      },
      {
        label: '万丹',
        value: 'IDN_BT',
      },
      {
        label: '西努沙登加拉',
        value: 'IDN_NB',
      },
      {
        label: '西苏门答腊',
        value: 'IDN_SR',
      },
      {
        label: '西爪哇',
        value: 'IDN_JB',
      },
      {
        label: '雅加达',
        value: 'IDN_JK',
      },
      {
        label: '亚齐',
        value: 'IDN_AC',
      },
      {
        label: '伊里安查亚',
        value: 'IDN_IJ',
      },
      {
        label: '占碑',
        value: 'IDN_JA',
      },
      {
        label: '中加里曼丹',
        value: 'IDN_KT',
      },
      {
        label: '中苏拉威西',
        value: 'IDN_ST',
      },
      {
        label: '中爪哇',
        value: 'IDN_JT',
      },
    ],
  },
  {
    label: '曼岛',
    value: 'IMN',
  },
  {
    label: '印尼',
    value: 'INA',
  },
  {
    label: '印度',
    value: 'IND',
    children: [
      {
        label: '艾藻尔',
        value: 'IND_AJL',
      },
      {
        label: '班加罗尔',
        value: 'IND_BLR',
      },
      {
        label: '本地治里',
        value: 'IND_PNY',
      },
      {
        label: '博帕尔',
        value: 'IND_BHO',
      },
      {
        label: '布巴内斯瓦尔',
        value: 'IND_BBI',
      },
      {
        label: '昌迪加尔',
        value: 'IND_IXC',
      },
      {
        label: '达曼',
        value: 'IND_DAM',
      },
      {
        label: '第乌',
        value: 'IND_DIU',
      },
      {
        label: '甘托克',
        value: 'IND_GTO',
      },
      {
        label: '哥印拜陀',
        value: 'IND_CJB',
      },
      {
        label: '加尔各答',
        value: 'IND_CCU',
      },
      {
        label: '加里加尔',
        value: 'IND_KRK',
      },
      {
        label: '贾巴尔普尔',
        value: 'IND_JLR',
      },
      {
        label: '贾朗达尔',
        value: 'IND_JUC',
      },
      {
        label: '焦特布尔',
        value: 'IND_JDH',
      },
      {
        label: '金奈',
        value: 'IND_MAA',
      },
      {
        label: '卡瓦拉蒂',
        value: 'IND_KVA',
      },
      {
        label: '科希马',
        value: 'IND_KOM',
      },
      {
        label: '马埃',
        value: 'IND_MAH',
      },
      {
        label: '马杜赖',
        value: 'IND_IXM',
      },
      {
        label: '森伯尔布尔',
        value: 'IND_SLR',
      },
      {
        label: '特里凡得琅',
        value: 'IND_TRV',
      },
      {
        label: '乌代布尔',
        value: 'IND_UDR',
      },
      {
        label: '西隆',
        value: 'IND_SHL',
      },
      {
        label: '锡尔萨瓦',
        value: 'IND_SIL',
      },
      {
        label: '新德里',
        value: 'IND_ICD',
      },
      {
        label: '亚南',
        value: 'IND_SRV',
      },
      {
        label: '因帕尔',
        value: 'IND_IMF',
      },
      {
        label: '印多尔',
        value: 'IND_IDR',
      },
      {
        label: '斋普尔',
        value: 'IND_JAI',
      },
    ],
  },
  {
    label: '英属印度洋领地',
    value: 'IOT',
  },
  {
    label: '伊朗',
    value: 'IRI',
  },
  {
    label: '爱尔兰',
    value: 'IRL',
    children: [
      {
        label: '奥法利',
        value: 'IRL_OF',
      },
      {
        label: '蒂珀雷里',
        value: 'IRL_TP',
      },
      {
        label: '都柏林',
        value: 'IRL_DB',
      },
      {
        label: '多内加尔',
        value: 'IRL_DG',
      },
      {
        label: '戈尔韦',
        value: 'IRL_GW',
      },
      {
        label: '基尔代尔',
        value: 'IRL_KD',
      },
      {
        label: '基尔肯尼',
        value: 'IRL_KK',
      },
      {
        label: '卡范',
        value: 'IRL_CV',
      },
      {
        label: '卡洛',
        value: 'IRL_CW',
      },
      {
        label: '凯里',
        value: 'IRL_KR',
      },
      {
        label: '科克',
        value: 'IRL_CK',
      },
      {
        label: '克莱尔',
        value: 'IRL_CL',
      },
      {
        label: '朗福德',
        value: 'IRL_LF',
      },
      {
        label: '劳斯',
        value: 'IRL_LT',
      },
      {
        label: '崂斯',
        value: 'IRL_LA',
      },
      {
        label: '利默里克',
        value: 'IRL_LM',
      },
      {
        label: '利特里姆',
        value: 'IRL_LR',
      },
      {
        label: '罗斯康芒',
        value: 'IRL_RC',
      },
      {
        label: '梅奥',
        value: 'IRL_MY',
      },
      {
        label: '米斯',
        value: 'IRL_MT',
      },
      {
        label: '莫内根',
        value: 'IRL_MG',
      },
      {
        label: '斯莱戈',
        value: 'IRL_SL',
      },
      {
        label: '威克洛',
        value: 'IRL_WK',
      },
      {
        label: '韦克斯福德',
        value: 'IRL_WX',
      },
      {
        label: '沃特福德',
        value: 'IRL_WF',
      },
      {
        label: '西米斯',
        value: 'IRL_WM',
      },
    ],
  },
  {
    label: '伊拉克',
    value: 'IRQ',
  },
  {
    label: '冰岛',
    value: 'ISL',
  },
  {
    label: '以色列',
    value: 'ISR',
    children: [
      {
        label: '阿什杜德',
        value: 'ISR_ASH',
      },
      {
        label: '贝尔谢巴',
        value: 'ISR_BEV',
      },
      {
        label: '贝特雁',
        value: 'ISR_BAT',
      },
      {
        label: '海法',
        value: 'ISR_HFA',
      },
      {
        label: '霍隆',
        value: 'ISR_HOL',
      },
      {
        label: '内坦亚',
        value: 'ISR_NAT',
      },
      {
        label: '特拉维夫',
        value: 'ISR_TLV',
      },
      {
        label: '耶路撒冷',
        value: 'ISR_J',
      },
    ],
  },
  {
    label: '意大利',
    value: 'ITA',
    children: [
      {
        label: '阿斯蒂',
        value: 'ITA_AST',
      },
      {
        label: '阿斯科利皮切诺',
        value: 'ITA_ASP',
      },
      {
        label: '安科纳',
        value: 'ITA_AOI',
      },
      {
        label: '奥尔比亚',
        value: 'ITA_OLB',
      },
      {
        label: '奥里斯塔诺',
        value: 'ITA_QOS',
      },
      {
        label: '奥斯塔',
        value: 'ITA_AOT',
      },
      {
        label: '巴勒莫',
        value: 'ITA_PMO',
      },
      {
        label: '巴里',
        value: 'ITA_BRI',
      },
      {
        label: '贝加莫',
        value: 'ITA_BGO',
      },
      {
        label: '贝内文托',
        value: 'ITA_BEN',
      },
      {
        label: '比萨',
        value: 'ITA_PSA',
      },
      {
        label: '波代诺内',
        value: 'ITA_PRD',
      },
      {
        label: '波坦察',
        value: 'ITA_QPO',
      },
      {
        label: '博洛尼亚',
        value: 'ITA_BLQ',
      },
      {
        label: '布拉',
        value: 'ITA_BIE',
      },
      {
        label: '布雷西亚',
        value: 'ITA_BRC',
      },
      {
        label: '布林迪西',
        value: 'ITA_BDS',
      },
      {
        label: '的里雅斯特',
        value: 'ITA_TRS',
      },
      {
        label: '都灵',
        value: 'ITA_TRN',
      },
      {
        label: '费拉拉',
        value: 'ITA_FRR',
      },
      {
        label: '佛罗伦萨',
        value: 'ITA_FLR',
      },
      {
        label: '福贾',
        value: 'ITA_FOG',
      },
      {
        label: '卡利亚里',
        value: 'ITA_CAG',
      },
      {
        label: '卡塞塔',
        value: 'ITA_CST',
      },
      {
        label: '卡塔尼亚',
        value: 'ITA_CTA',
      },
      {
        label: '卡坦扎罗',
        value: 'ITA_QCZ',
      },
      {
        label: '坎波巴索',
        value: 'ITA_COB',
      },
      {
        label: '科摩',
        value: 'ITA_CIY',
      },
      {
        label: '科森扎',
        value: 'ITA_QCS',
      },
      {
        label: '克罗托内',
        value: 'ITA_CRV',
      },
      {
        label: '库内奥',
        value: 'ITA_CUN',
      },
      {
        label: '拉奎拉',
        value: 'ITA_LAQ',
      },
      {
        label: '拉斯佩齐亚',
        value: 'ITA_SPE',
      },
      {
        label: '莱科',
        value: 'ITA_LCO',
      },
      {
        label: '莱切',
        value: 'ITA_LCC',
      },
      {
        label: '雷焦艾米利亚',
        value: 'ITA_RNE',
      },
      {
        label: '雷焦卡拉布里亚',
        value: 'ITA_REG',
      },
      {
        label: '里窝那',
        value: 'ITA_LIV',
      },
      {
        label: '罗马',
        value: 'ITA_ROM',
      },
      {
        label: '马萨',
        value: 'ITA_MCR',
      },
      {
        label: '马泰拉',
        value: 'ITA_MTR',
      },
      {
        label: '蒙扎',
        value: 'ITA_MZA',
      },
      {
        label: '米兰',
        value: 'ITA_MIL',
      },
      {
        label: '摩德纳',
        value: 'ITA_MOD',
      },
      {
        label: '墨西拿',
        value: 'ITA_MSN',
      },
      {
        label: '那不勒斯',
        value: 'ITA_NAP',
      },
      {
        label: '努奥罗',
        value: 'ITA_QNU',
      },
      {
        label: '诺瓦拉',
        value: 'ITA_NVR',
      },
      {
        label: '帕尔马',
        value: 'ITA_PMF',
      },
      {
        label: '帕维亚',
        value: 'ITA_PAV',
      },
      {
        label: '佩鲁贾',
        value: 'ITA_PEG',
      },
      {
        label: '热那亚',
        value: 'ITA_CAX',
      },
      {
        label: '萨莱诺',
        value: 'ITA_SAL',
      },
      {
        label: '萨萨里',
        value: 'ITA_QSS',
      },
      {
        label: '萨沃纳',
        value: 'ITA_SVN',
      },
      {
        label: '塔兰托',
        value: 'ITA_TAR',
      },
      {
        label: '特拉帕尼',
        value: 'ITA_TPS',
      },
      {
        label: '特伦托',
        value: 'ITA_TRT',
      },
      {
        label: '威尼斯',
        value: 'ITA_VCE',
      },
      {
        label: '韦尔切利',
        value: 'ITA_VRL',
      },
      {
        label: '维泰博',
        value: 'ITA_VIT',
      },
      {
        label: '乌迪内',
        value: 'ITA_UDN',
      },
      {
        label: '锡拉库扎',
        value: 'ITA_SYR',
      },
      {
        label: '锡耶纳',
        value: 'ITA_SNA',
      },
      {
        label: '亚历山德里亚',
        value: 'ITA_ALE',
      },
      {
        label: '伊塞尔尼亚',
        value: 'ITA_ISE',
      },
    ],
  },
  {
    label: '牙买加',
    value: 'JAM',
    children: [
      {
        label: '波特兰',
        value: 'JAM_POR',
      },
      {
        label: '汉诺威',
        value: 'JAM_HAN',
      },
      {
        label: '金斯敦',
        value: 'JAM_KIN',
      },
      {
        label: '克拉伦登',
        value: 'JAM_CLA',
      },
      {
        label: '曼彻斯特',
        value: 'JAM_MAN',
      },
      {
        label: '圣安德鲁斯',
        value: 'JAM_AND',
      },
      {
        label: '圣安娜',
        value: 'JAM_ANN',
      },
      {
        label: '圣凯瑟琳',
        value: 'JAM_CAT',
      },
      {
        label: '圣玛丽',
        value: 'JAM_MAR',
      },
      {
        label: '圣托马斯',
        value: 'JAM_THO',
      },
      {
        label: '圣伊丽莎白',
        value: 'JAM_ELI',
      },
      {
        label: '圣詹姆斯',
        value: 'JAM_JAM',
      },
      {
        label: '特里洛尼',
        value: 'JAM_TRL',
      },
      {
        label: '西摩兰',
        value: 'JAM_WML',
      },
    ],
  },
  {
    label: '泽西岛',
    value: 'JEY',
  },
  {
    label: '约旦',
    value: 'JOR',
    children: [
      {
        label: '阿吉隆',
        value: 'JOR_AJ',
      },
      {
        label: '安曼',
        value: 'JOR_AM',
      },
      {
        label: '拜勒加',
        value: 'JOR_BA',
      },
      {
        label: '杰拉什',
        value: 'JOR_JA',
      },
      {
        label: '卡拉克',
        value: 'JOR_KA',
      },
      {
        label: '鲁赛法',
        value: 'JOR_RU',
      },
      {
        label: '马安',
        value: 'JOR_MN',
      },
      {
        label: '马德巴',
        value: 'JOR_MD',
      },
      {
        label: '马夫拉克',
        value: 'JOR_MF',
      },
      {
        label: '塔菲拉',
        value: 'JOR_TA',
      },
      {
        label: '亚喀巴',
        value: 'JOR_AQ',
      },
      {
        label: '伊尔比德',
        value: 'JOR_IR',
      },
      {
        label: '扎尔卡',
        value: 'JOR_ZA',
      },
    ],
  },
  {
    label: '日本',
    value: 'JPN',
    children: [
      {
        label: '北海道',
        value: 'JPN_1',
      },
      {
        label: '青森',
        value: 'JPN_2',
      },
      {
        label: '岩手',
        value: 'JPN_3',
      },
      {
        label: '宮城',
        value: 'JPN_4',
      },
      {
        label: '秋田',
        value: 'JPN_5',
      },
      {
        label: '山形',
        value: 'JPN_6',
      },
      {
        label: '福岛',
        value: 'JPN_7',
      },
      {
        label: '茨城',
        value: 'JPN_8',
      },
      {
        label: '枥木',
        value: 'JPN_9',
      },
      {
        label: '群马',
        value: 'JPN_10',
      },
      {
        label: '埼玉',
        value: 'JPN_11',
      },
      {
        label: '千叶',
        value: 'JPN_12',
      },
      {
        label: '东京',
        value: 'JPN_13',
      },
      {
        label: '神奈川',
        value: 'JPN_14',
      },
      {
        label: '新潟',
        value: 'JPN_15',
      },
      {
        label: '富山',
        value: 'JPN_16',
      },
      {
        label: '石川',
        value: 'JPN_17',
      },
      {
        label: '福井',
        value: 'JPN_18',
      },
      {
        label: '山梨',
        value: 'JPN_19',
      },
      {
        label: '长野',
        value: 'JPN_20',
      },
      {
        label: '岐阜',
        value: 'JPN_21',
      },
      {
        label: '静冈',
        value: 'JPN_22',
      },
      {
        label: '爱知',
        value: 'JPN_23',
      },
      {
        label: '三重',
        value: 'JPN_24',
      },
      {
        label: '滋贺',
        value: 'JPN_25',
      },
      {
        label: '京都',
        value: 'JPN_26',
      },
      {
        label: '大阪',
        value: 'JPN_27',
      },
      {
        label: '兵库',
        value: 'JPN_28',
      },
      {
        label: '奈良',
        value: 'JPN_29',
      },
      {
        label: '和歌山',
        value: 'JPN_30',
      },
      {
        label: '鸟取',
        value: 'JPN_31',
      },
      {
        label: '岛根',
        value: 'JPN_32',
      },
      {
        label: '冈山',
        value: 'JPN_33',
      },
      {
        label: '广岛',
        value: 'JPN_34',
      },
      {
        label: '山口',
        value: 'JPN_35',
      },
      {
        label: '徳岛',
        value: 'JPN_36',
      },
      {
        label: '香川',
        value: 'JPN_37',
      },
      {
        label: '爱媛',
        value: 'JPN_38',
      },
      {
        label: '高知',
        value: 'JPN_39',
      },
      {
        label: '福冈',
        value: 'JPN_40',
      },
      {
        label: '佐贺',
        value: 'JPN_41',
      },
      {
        label: '长崎',
        value: 'JPN_42',
      },
      {
        label: '熊本',
        value: 'JPN_43',
      },
      {
        label: '大分',
        value: 'JPN_44',
      },
      {
        label: '宫崎',
        value: 'JPN_45',
      },
      {
        label: '鹿儿岛',
        value: 'JPN_46',
      },
      {
        label: '冲绳',
        value: 'JPN_47',
      },
    ],
  },
  {
    label: '哈萨克斯坦',
    value: 'KAZ',
    children: [
      {
        label: '阿尔卡累克',
        value: 'KAZ_AYK',
      },
      {
        label: '阿克莫拉',
        value: 'KAZ_AKM',
      },
      {
        label: '阿克苏',
        value: 'KAZ_AKS',
      },
      {
        label: '阿克托别',
        value: 'KAZ_AKT',
      },
      {
        label: '阿拉木图',
        value: 'KAZ_ALA',
      },
      {
        label: '阿雷斯',
        value: 'KAZ_ARY',
      },
      {
        label: '阿斯塔纳市',
        value: 'KAZ_AST',
      },
      {
        label: '阿特劳',
        value: 'KAZ_ATY',
      },
      {
        label: '埃基巴斯图兹',
        value: 'KAZ_EKB',
      },
      {
        label: '巴尔喀什',
        value: 'KAZ_BXH',
      },
      {
        label: '巴甫洛达尔',
        value: 'KAZ_PAV',
      },
      {
        label: '北哈萨克斯坦',
        value: 'KAZ_SEV',
      },
      {
        label: '东哈萨克斯坦',
        value: 'KAZ_VOS',
      },
      {
        label: '济良诺夫斯克',
        value: 'KAZ_ZYR',
      },
      {
        label: '江布尔',
        value: 'KAZ_DMB',
      },
      {
        label: '杰兹卡兹甘',
        value: 'KAZ_DZH',
      },
      {
        label: '卡拉干达',
        value: 'KAZ_KAR',
      },
      {
        label: '卡拉扎尔',
        value: 'KAZ_KZO',
      },
      {
        label: '卡普恰盖',
        value: 'KAZ_KAP',
      },
      {
        label: '科斯塔奈',
        value: 'KAZ_KST',
      },
      {
        label: '克孜勒奥尔达',
        value: 'KAZ_KZY',
      },
      {
        label: '库尔恰托夫',
        value: 'KAZ_KUR',
      },
      {
        label: '利萨科夫斯克',
        value: 'KAZ_LKK',
      },
      {
        label: '列宁诺戈尔斯克',
        value: 'KAZ_LEN',
      },
      {
        label: '鲁德内',
        value: 'KAZ_RUD',
      },
      {
        label: '曼格斯套',
        value: 'KAZ_MAN',
      },
      {
        label: '南哈萨克斯坦',
        value: 'KAZ_KGT',
      },
      {
        label: '萨兰',
        value: 'KAZ_SAR',
      },
      {
        label: '塞梅伊',
        value: 'KAZ_SEM',
      },
      {
        label: '沙赫京斯克',
        value: 'KAZ_SAK',
      },
      {
        label: '斯捷普诺戈尔斯克',
        value: 'KAZ_STE',
      },
      {
        label: '铁克利',
        value: 'KAZ_TEK',
      },
      {
        label: '铁米尔套',
        value: 'KAZ_TEM',
      },
      {
        label: '突厥斯坦',
        value: 'KAZ_TUR',
      },
      {
        label: '西哈萨克斯坦',
        value: 'KAZ_ZAP',
      },
      {
        label: '扎纳奥津',
        value: 'KAZ_ZHA',
      },
    ],
  },
  {
    label: '肯尼亚',
    value: 'KEN',
    children: [
      {
        label: '埃尔格约-马拉奎特',
        value: 'KEN_EMA',
      },
      {
        label: '巴林戈',
        value: 'KEN_BAR',
      },
      {
        label: '邦戈马',
        value: 'KEN_BUN',
      },
      {
        label: '博美特',
        value: 'KEN_BOM',
      },
      {
        label: '布希亚',
        value: 'KEN_BUS',
      },
      {
        label: '恩布',
        value: 'KEN_EMB',
      },
      {
        label: '霍马湾',
        value: 'KEN_HOB',
      },
      {
        label: '基安布',
        value: 'KEN_KIA',
      },
      {
        label: '基里菲',
        value: 'KEN_KIL',
      },
      {
        label: '基里尼亚加',
        value: 'KEN_KIR',
      },
      {
        label: '基苏木',
        value: 'KEN_KIS',
      },
      {
        label: '基图伊',
        value: 'KEN_KIT',
      },
      {
        label: '基西',
        value: 'KEN_KII',
      },
      {
        label: '加里萨',
        value: 'KEN_GAS',
      },
      {
        label: '卡卡梅加',
        value: 'KEN_KAK',
      },
      {
        label: '卡耶亚多',
        value: 'KEN_KAJ',
      },
      {
        label: '凯里乔',
        value: 'KEN_KEY',
      },
      {
        label: '夸勒',
        value: 'KEN_KWA',
      },
      {
        label: '拉木',
        value: 'KEN_LAU',
      },
      {
        label: '莱基皮亚',
        value: 'KEN_LAI',
      },
      {
        label: '马查科斯',
        value: 'KEN_MAC',
      },
      {
        label: '马瓜尼',
        value: 'KEN_MAK',
      },
      {
        label: '马萨布布',
        value: 'KEN_RBT',
      },
      {
        label: '曼德拉',
        value: 'KEN_MAN',
      },
      {
        label: '梅鲁',
        value: 'KEN_MER',
      },
      {
        label: '蒙巴萨',
        value: 'KEN_MOM',
      },
      {
        label: '米戈利',
        value: 'KEN_MIG',
      },
      {
        label: '穆兰卡',
        value: 'KEN_MUR',
      },
      {
        label: '纳库鲁',
        value: 'KEN_NUU',
      },
      {
        label: '纳罗克',
        value: 'KEN_NAR',
      },
      {
        label: '南迪',
        value: 'KEN_NAN',
      },
      {
        label: '内罗毕',
        value: 'KEN_NA',
      },
      {
        label: '尼蒂',
        value: 'KEN_NIT',
      },
      {
        label: '尼亚米拉',
        value: 'KEN_NYM',
      },
      {
        label: '年达鲁阿',
        value: 'KEN_NYN',
      },
      {
        label: '涅里',
        value: 'KEN_NYE',
      },
      {
        label: '桑布卢',
        value: 'KEN_UAS',
      },
      {
        label: '塔纳河',
        value: 'KEN_TRI',
      },
      {
        label: '泰塔塔维塔',
        value: 'KEN_TTA',
      },
      {
        label: '特兰斯-恩佐亚',
        value: 'KEN_TNZ',
      },
      {
        label: '瓦吉尔',
        value: 'KEN_WJR',
      },
      {
        label: '瓦辛基苏',
        value: 'KEN_UGI',
      },
      {
        label: '韦希加',
        value: 'KEN_VIH',
      },
      {
        label: '西波克特',
        value: 'KEN_WPO',
      },
      {
        label: '夏亚',
        value: 'KEN_SIA',
      },
      {
        label: '伊希约洛',
        value: 'KEN_ISI',
      },
      {
        label: '中央',
        value: 'KEN_CE',
      },
    ],
  },
  {
    label: '吉尔吉斯斯坦',
    value: 'KGZ',
    children: [
      {
        label: '奥什',
        value: 'KGZ_O',
      },
      {
        label: '巴特肯',
        value: 'KGZ_B',
      },
      {
        label: '比什凯克市',
        value: 'KGZ_GB',
      },
      {
        label: '楚河',
        value: 'KGZ_C',
      },
      {
        label: '贾拉拉巴德',
        value: 'KGZ_J',
      },
      {
        label: '卡拉巴尔塔',
        value: 'KGZ_KBA',
      },
      {
        label: '卡拉库尔',
        value: 'KGZ_KKO',
      },
      {
        label: '坎特',
        value: 'KGZ_KAN',
      },
      {
        label: '科克扬加克',
        value: 'KGZ_KJ',
      },
      {
        label: '迈利赛',
        value: 'KGZ_MS',
      },
      {
        label: '纳伦',
        value: 'KGZ_N',
      },
      {
        label: '苏卢克图',
        value: 'KGZ_SU',
      },
      {
        label: '塔拉斯',
        value: 'KGZ_T',
      },
      {
        label: '塔什库梅尔',
        value: 'KGZ_TK',
      },
      {
        label: '乌兹根',
        value: 'KGZ_UG',
      },
      {
        label: '伊塞克湖',
        value: 'KGZ_Y',
      },
    ],
  },
  {
    label: '基里巴斯',
    value: 'KIR',
    children: [
      {
        label: '菲尼克斯群岛',
        value: 'KIR_PHO',
      },
      {
        label: '吉尔伯特群岛',
        value: 'KIR_GIL',
      },
      {
        label: '莱恩群岛',
        value: 'KIR_LIN',
      },
    ],
  },
  {
    label: '韩国',
    value: 'KOR',
    children: [
      {
        label: '首尔',
        value: 'KOR_11',
      },
      {
        label: '釜山',
        value: 'KOR_26',
      },
      {
        label: '大邱',
        value: 'KOR_27',
      },
      {
        label: '仁川',
        value: 'KOR_28',
      },
      {
        label: '光州',
        value: 'KOR_29',
      },
      {
        label: '大田',
        value: 'KOR_30',
      },
      {
        label: '蔚山',
        value: 'KOR_31',
      },
      {
        label: '济州特别自治道',
        value: 'KOR_41',
      },
      {
        label: '江原道',
        value: 'KOR_42',
      },
      {
        label: '庆尚北道',
        value: 'KOR_43',
      },
      {
        label: '庆尚南道',
        value: 'KOR_44',
      },
      {
        label: '忠清北道',
        value: 'KOR_45',
      },
      {
        label: '忠清南道',
        value: 'KOR_46',
      },
      {
        label: '全罗北道',
        value: 'KOR_47',
      },
      {
        label: '全罗南道',
        value: 'KOR_48',
      },
      {
        label: '京畿道',
        value: 'KOR_49',
      },
    ],
  },
  {
    label: '沙特阿拉伯',
    value: 'KSA',
  },
  {
    label: '科威特',
    value: 'KUW',
  },
  {
    label: '老挝',
    value: 'LAO',
    children: [
      {
        label: '阿速坡',
        value: 'LAO_AT',
      },
      {
        label: '波里坎赛',
        value: 'LAO_BL',
      },
      {
        label: '博乔',
        value: 'LAO_BK',
      },
      {
        label: '川圹',
        value: 'LAO_XI',
      },
      {
        label: '丰沙里',
        value: 'LAO_PH',
      },
      {
        label: '甘蒙',
        value: 'LAO_KH',
      },
      {
        label: '华潘',
        value: 'LAO_HO',
      },
      {
        label: '琅勃拉邦',
        value: 'LAO_LP',
      },
      {
        label: '琅南塔',
        value: 'LAO_LM',
      },
      {
        label: '赛宋本行政特区',
        value: 'LAO_XN',
      },
      {
        label: '色贡',
        value: 'LAO_XE',
      },
      {
        label: '沙拉湾',
        value: 'LAO_SL',
      },
      {
        label: '沙湾拿吉',
        value: 'LAO_SV',
      },
      {
        label: '沙耶武里',
        value: 'LAO_XA',
      },
      {
        label: '万象',
        value: 'LAO_VI',
      },
      {
        label: '乌多姆赛',
        value: 'LAO_OU',
      },
      {
        label: '占巴塞',
        value: 'LAO_CH',
      },
    ],
  },
  {
    label: '拉脱维亚',
    value: 'LAT',
  },
  {
    label: '利比亚',
    value: 'LBA',
  },
  {
    label: '利比里亚',
    value: 'LBR',
    children: [
      {
        label: '巴波卢',
        value: 'LBR_GBA',
      },
      {
        label: '邦',
        value: 'LBR_BG',
      },
      {
        label: '博波卢',
        value: 'LBR_BOP',
      },
      {
        label: '博米',
        value: 'LBR_BM',
      },
      {
        label: '大巴萨',
        value: 'LBR_GB',
      },
      {
        label: '大吉德',
        value: 'LBR_GG',
      },
      {
        label: '大角山',
        value: 'LBR_CM',
      },
      {
        label: '大克鲁',
        value: 'LBR_GK',
      },
      {
        label: '菲什敦',
        value: 'LBR_FT',
      },
      {
        label: '吉河',
        value: 'LBR_RG',
      },
      {
        label: '里弗塞斯',
        value: 'LBR_RI',
      },
      {
        label: '洛法',
        value: 'LBR_LO',
      },
      {
        label: '马吉比',
        value: 'LBR_MG',
      },
      {
        label: '马里兰',
        value: 'LBR_MY',
      },
      {
        label: '蒙特塞拉多',
        value: 'LBR_MO',
      },
      {
        label: '宁巴',
        value: 'LBR_NI',
      },
      {
        label: '锡诺',
        value: 'LBR_SI',
      },
    ],
  },
  {
    label: '圣卢西亚',
    value: 'LCA',
  },
  {
    label: '莱索托',
    value: 'LES',
  },
  {
    label: '黎巴嫩',
    value: 'LIB',
  },
  {
    label: '列支敦士登',
    value: 'LIE',
  },
  {
    label: '立陶宛',
    value: 'LTU',
    children: [
      {
        label: '阿利图斯',
        value: 'LTU_AL',
      },
      {
        label: '考纳斯',
        value: 'LTU_KA',
      },
      {
        label: '克莱佩达',
        value: 'LTU_KL',
      },
      {
        label: '马里扬泊列',
        value: 'LTU_MA',
      },
      {
        label: '帕涅韦日斯',
        value: 'LTU_PA',
      },
      {
        label: '陶拉格',
        value: 'LTU_TA',
      },
      {
        label: '特尔希艾',
        value: 'LTU_TE',
      },
      {
        label: '维尔纽斯',
        value: 'LTU_VI',
      },
      {
        label: '乌田纳',
        value: 'LTU_UT',
      },
      {
        label: '希奥利艾',
        value: 'LTU_SI',
      },
      {
        label: '亚克曼',
        value: 'LTU_AKM',
      },
    ],
  },
  {
    label: '卢森堡',
    value: 'LUX',
    children: [
      {
        label: '迪基希',
        value: 'LUX_DD',
      },
      {
        label: '格雷文马赫',
        value: 'LUX_GG',
      },
      {
        label: '卢森堡',
        value: 'LUX_LL',
      },
    ],
  },
  {
    label: '马达加斯加',
    value: 'MAD',
  },
  {
    label: '摩洛哥',
    value: 'MAR',
    children: [
      {
        label: '丹吉尔',
        value: 'MAR_TGR',
      },
      {
        label: '得土安',
        value: 'MAR_TET',
      },
      {
        label: '非斯',
        value: 'MAR_FES',
      },
      {
        label: '卡萨布兰卡',
        value: 'MAR_CBL',
      },
      {
        label: '拉巴特',
        value: 'MAR_RSA',
      },
      {
        label: '马拉喀什',
        value: 'MAR_MRK',
      },
      {
        label: '梅克内斯',
        value: 'MAR_MKN',
      },
      {
        label: '乌季达',
        value: 'MAR_OUJ',
      },
      {
        label: '西撒哈拉',
        value: 'MAR_WSH',
      },
    ],
  },
  {
    label: '马来西亚',
    value: 'MAS',
  },
  {
    label: '马拉维',
    value: 'MAW',
  },
  {
    label: '摩尔多瓦',
    value: 'MDA',
  },
  {
    label: '马尔代夫',
    value: 'MDV',
    children: [
      {
        label: '阿杜',
        value: 'MDV_ADD',
      },
      {
        label: '北阿里',
        value: 'MDV_AAD',
      },
      {
        label: '北蒂拉杜马蒂',
        value: 'MDV_THD',
      },
      {
        label: '北马洛斯马杜卢',
        value: 'MDV_MAD',
      },
      {
        label: '北米拉杜马杜卢',
        value: 'MDV_MLD',
      },
      {
        label: '北尼兰杜',
        value: 'MDV_NAD',
      },
      {
        label: '北苏瓦迪瓦',
        value: 'MDV_HAD',
      },
      {
        label: '法迪福卢',
        value: 'MDV_FAA',
      },
      {
        label: '费利杜',
        value: 'MDV_FEA',
      },
      {
        label: '福阿穆拉库',
        value: 'MDV_FMU',
      },
      {
        label: '哈杜马蒂',
        value: 'MDV_HDH',
      },
      {
        label: '科卢马杜卢',
        value: 'MDV_KLH',
      },
      {
        label: '马累',
        value: 'MDV_MAL',
      },
      {
        label: '马累岛',
        value: 'MDV_MAA',
      },
      {
        label: '穆拉库',
        value: 'MDV_MUA',
      },
      {
        label: '南阿里',
        value: 'MDV_AAU',
      },
      {
        label: '南蒂拉杜马蒂',
        value: 'MDV_THU',
      },
      {
        label: '南马洛斯马杜卢',
        value: 'MDV_MAU',
      },
      {
        label: '南米拉杜马杜卢',
        value: 'MDV_MLU',
      },
      {
        label: '南尼兰杜',
        value: 'MDV_NAU',
      },
      {
        label: '南苏瓦迪瓦',
        value: 'MDV_HAU',
      },
    ],
  },
  {
    label: '墨西哥',
    value: 'MEX',
    children: [
      {
        label: '阿瓜斯卡连斯特',
        value: 'MEX_AGU',
      },
      {
        label: '阿卡普尔科',
        value: 'MEX_ACA',
      },
      {
        label: '埃莫西约',
        value: 'MEX_HMO',
      },
      {
        label: '埃佩切',
        value: 'MEX_CAM',
      },
      {
        label: '奥夫雷贡城',
        value: 'MEX_OBR',
      },
      {
        label: '奥里萨巴',
        value: 'MEX_ORI',
      },
      {
        label: '巴利城',
        value: 'MEX_VHM',
      },
      {
        label: '巴亚尔塔港',
        value: 'MEX_PVR',
      },
      {
        label: '比利亚埃尔莫萨',
        value: 'MEX_VSA',
      },
      {
        label: '波萨里卡',
        value: 'MEX_PRH',
      },
      {
        label: '蒂华纳',
        value: 'MEX_TIJ',
      },
      {
        label: '杜兰戈',
        value: 'MEX_DUR',
      },
      {
        label: '恩塞纳达',
        value: 'MEX_ESE',
      },
      {
        label: '瓜达拉哈拉',
        value: 'MEX_GDL',
      },
      {
        label: '瓜纳华托',
        value: 'MEX_GUA',
      },
      {
        label: '哈拉帕',
        value: 'MEX_JAL',
      },
      {
        label: '华雷斯',
        value: 'MEX_JUZ',
      },
      {
        label: '华雷斯港',
        value: 'MEX_BJU',
      },
      {
        label: '卡门',
        value: 'MEX_CAR',
      },
      {
        label: '科利马',
        value: 'MEX_COL',
      },
      {
        label: '克雷塔罗',
        value: 'MEX_QUE',
      },
      {
        label: '库埃纳瓦卡',
        value: 'MEX_CVC',
      },
      {
        label: '库利阿坎',
        value: 'MEX_CUL',
      },
      {
        label: '夸察夸拉克斯',
        value: 'MEX_COA',
      },
      {
        label: '拉巴斯',
        value: 'MEX_LAP',
      },
      {
        label: '莱昂',
        value: 'MEX_LEN',
      },
      {
        label: '雷诺萨',
        value: 'MEX_REX',
      },
      {
        label: '洛斯莫奇斯',
        value: 'MEX_LMM',
      },
      {
        label: '马萨特兰',
        value: 'MEX_MZT',
      },
      {
        label: '马塔莫罗斯',
        value: 'MEX_MAM',
      },
      {
        label: '梅里达',
        value: 'MEX_MID',
      },
      {
        label: '蒙克洛瓦',
        value: 'MEX_LOV',
      },
      {
        label: '蒙特雷',
        value: 'MEX_MTY',
      },
      {
        label: '莫雷利亚',
        value: 'MEX_MLM',
      },
      {
        label: '墨西哥城',
        value: 'MEX_MEX',
      },
      {
        label: '墨西卡利',
        value: 'MEX_MXL',
      },
      {
        label: '诺加莱斯',
        value: 'MEX_NOG',
      },
      {
        label: '帕丘卡',
        value: 'MEX_PAC',
      },
      {
        label: '普埃布拉',
        value: 'MEX_PUE',
      },
      {
        label: '奇尔潘辛戈',
        value: 'MEX_CHI',
      },
      {
        label: '奇瓦瓦',
        value: 'MEX_CHH',
      },
      {
        label: '切图马尔',
        value: 'MEX_CTM',
      },
      {
        label: '萨尔蒂约',
        value: 'MEX_SLW',
      },
      {
        label: '萨卡特卡斯',
        value: 'MEX_ZAC',
      },
      {
        label: '塞拉亚',
        value: 'MEX_CLY',
      },
      {
        label: '圣路易斯波托亚',
        value: 'MEX_SLP',
      },
      {
        label: '塔帕丘拉',
        value: 'MEX_TAP',
      },
      {
        label: '坦皮科',
        value: 'MEX_TAM',
      },
      {
        label: '特拉斯卡拉',
        value: 'MEX_TLA',
      },
      {
        label: '特皮克',
        value: 'MEX_TPQ',
      },
      {
        label: '特瓦坎',
        value: 'MEX_TCN',
      },
      {
        label: '图斯特拉-古铁雷斯',
        value: 'MEX_TGZ',
      },
      {
        label: '托雷翁',
        value: 'MEX_TRC',
      },
      {
        label: '托卢卡',
        value: 'MEX_TLC',
      },
      {
        label: '瓦哈卡',
        value: 'MEX_OAX',
      },
      {
        label: '维多利亚城',
        value: 'MEX_VIC',
      },
      {
        label: '韦拉克鲁斯',
        value: 'MEX_VER',
      },
      {
        label: '乌鲁阿潘',
        value: 'MEX_UPN',
      },
      {
        label: '新拉雷多',
        value: 'MEX_NLE',
      },
      {
        label: '伊拉普阿托',
        value: 'MEX_IRP',
      },
    ],
  },
  {
    label: '蒙古',
    value: 'MGL',
  },
  {
    label: '马其顿',
    value: 'MKD',
  },
  {
    label: '马里',
    value: 'MLI',
    children: [
      {
        label: '巴马科首都区',
        value: 'MLI_CD',
      },
      {
        label: '基达尔',
        value: 'MLI_KD',
      },
      {
        label: '加奥',
        value: 'MLI_GA',
      },
      {
        label: '卡伊',
        value: 'MLI_KY',
      },
      {
        label: '库利科罗',
        value: 'MLI_KL',
      },
      {
        label: '莫普提',
        value: 'MLI_MP',
      },
      {
        label: '塞古',
        value: 'MLI_SG',
      },
      {
        label: '通布图',
        value: 'MLI_TB',
      },
      {
        label: '锡卡索',
        value: 'MLI_SK',
      },
    ],
  },
  {
    label: '马耳他',
    value: 'MLT',
  },
  {
    label: '北马里亚纳群岛',
    value: 'MNP',
  },
  {
    label: '摩纳哥',
    value: 'MON',
  },
  {
    label: '莫桑比克',
    value: 'MOZ',
  },
  {
    label: '毛里求斯',
    value: 'MRI',
  },
  {
    label: '蒙特塞拉特',
    value: 'MSR',
  },
  {
    label: '毛里塔尼亚',
    value: 'MTN',
  },
  {
    label: '马提尼克',
    value: 'MTQ',
  },
  {
    label: '缅甸',
    value: 'MYA',
  },
  {
    label: '马约特岛',
    value: 'MYT',
  },
  {
    label: '纳米比亚',
    value: 'NAM',
    children: [
      {
        label: '埃龙戈',
        value: 'NAM_ER',
      },
      {
        label: '奥汉圭纳',
        value: 'NAM_OW',
      },
      {
        label: '奥卡万戈',
        value: 'NAM_KV',
      },
      {
        label: '奥马赫科',
        value: 'NAM_OK',
      },
      {
        label: '奥姆沙蒂',
        value: 'NAM_OT',
      },
      {
        label: '奥乔宗蒂约巴',
        value: 'NAM_OJ',
      },
      {
        label: '奥沙纳',
        value: 'NAM_ON',
      },
      {
        label: '奥希科托',
        value: 'NAM_OO',
      },
      {
        label: '哈达普',
        value: 'NAM_HA',
      },
      {
        label: '霍马斯',
        value: 'NAM_KH',
      },
      {
        label: '卡拉斯',
        value: 'NAM_KR',
      },
      {
        label: '卡普里维',
        value: 'NAM_CA',
      },
      {
        label: '库内内',
        value: 'NAM_KU',
      },
    ],
  },
  {
    label: '尼加拉瓜',
    value: 'NCA',
  },
  {
    label: '新喀里多尼亚',
    value: 'NCL',
  },
  {
    label: '荷兰',
    value: 'NED',
  },
  {
    label: '尼泊尔',
    value: 'NEP',
  },
  {
    label: '诺福克',
    value: 'NFK',
  },
  {
    label: '尼日利亚',
    value: 'NGR',
  },
  {
    label: '尼日尔',
    value: 'NIG',
  },
  {
    label: '纽埃',
    value: 'NIU',
  },
  {
    label: '挪威',
    value: 'NOR',
    children: [
      {
        label: '东福尔',
        value: 'NOR_1',
      },
      {
        label: '阿克什胡斯',
        value: 'NOR_2',
      },
      {
        label: '奥斯陆市',
        value: 'NOR_3',
      },
      {
        label: '海德马克',
        value: 'NOR_4',
      },
      {
        label: '奥普兰',
        value: 'NOR_5',
      },
      {
        label: '布斯克吕',
        value: 'NOR_6',
      },
      {
        label: '西福尔',
        value: 'NOR_7',
      },
      {
        label: '泰勒马克',
        value: 'NOR_8',
      },
      {
        label: '东阿格德尔',
        value: 'NOR_9',
      },
      {
        label: '西阿格德尔',
        value: 'NOR_10',
      },
      {
        label: '罗加兰',
        value: 'NOR_11',
      },
      {
        label: '霍达兰',
        value: 'NOR_12',
      },
      {
        label: '松恩－菲尤拉讷',
        value: 'NOR_14',
      },
      {
        label: '默勒－鲁姆斯达尔',
        value: 'NOR_15',
      },
      {
        label: '南特伦德拉格',
        value: 'NOR_16',
      },
      {
        label: '北特伦德拉格',
        value: 'NOR_17',
      },
      {
        label: '诺尔兰',
        value: 'NOR_18',
      },
      {
        label: '特罗姆斯',
        value: 'NOR_19',
      },
      {
        label: '芬马克',
        value: 'NOR_20',
      },
    ],
  },
  {
    label: '瑙鲁',
    value: 'NRU',
  },
  {
    label: '新西兰',
    value: 'NZL',
    children: [
      {
        label: '奥克兰',
        value: 'NZL_AUK',
      },
      {
        label: '北岸',
        value: 'NZL_NSH',
      },
      {
        label: '北帕默斯顿',
        value: 'NZL_PMR',
      },
      {
        label: '北远',
        value: 'NZL_FNR',
      },
      {
        label: '布莱尼姆',
        value: 'NZL_BHE',
      },
      {
        label: '达尼丁',
        value: 'NZL_DUD',
      },
      {
        label: '格雷茅斯',
        value: 'NZL_GMN',
      },
      {
        label: '哈密尔顿',
        value: 'NZL_HLZ',
      },
      {
        label: '黑斯廷斯',
        value: 'NZL_HAS',
      },
      {
        label: '怀塔科拉',
        value: 'NZL_WAE',
      },
      {
        label: '吉斯伯恩',
        value: 'NZL_GIS',
      },
      {
        label: '凯帕拉',
        value: 'NZL_KAI',
      },
      {
        label: '克赖斯特彻奇',
        value: 'NZL_CHC',
      },
      {
        label: '里士满',
        value: 'NZL_RMD',
      },
      {
        label: '马努考',
        value: 'NZL_MNK',
      },
      {
        label: '纳尔逊',
        value: 'NZL_NSN',
      },
      {
        label: '内皮尔',
        value: 'NZL_NPE',
      },
      {
        label: '斯特拉特福德',
        value: 'NZL_STR',
      },
      {
        label: '陶马鲁努伊',
        value: 'NZL_TAU',
      },
      {
        label: '瓦卡塔尼',
        value: 'NZL_WHK',
      },
      {
        label: '旺阿雷',
        value: 'NZL_WRE',
      },
      {
        label: '旺格努伊',
        value: 'NZL_WAG',
      },
      {
        label: '新普利茅斯',
        value: 'NZL_NPL',
      },
      {
        label: '因弗卡吉尔',
        value: 'NZL_IVC',
      },
    ],
  },
  {
    label: '阿曼',
    value: 'OMA',
  },
  {
    label: '巴基斯坦',
    value: 'PAK',
    children: [
      {
        label: '白沙瓦',
        value: 'PAK_PEW',
      },
      {
        label: '费萨拉巴德',
        value: 'PAK_LYP',
      },
      {
        label: '故吉软瓦拉',
        value: 'PAK_GUJ',
      },
      {
        label: '海德拉巴',
        value: 'PAK_HDD',
      },
      {
        label: '卡拉奇',
        value: 'PAK_KCT',
      },
      {
        label: '拉合尔',
        value: 'PAK_LHE',
      },
      {
        label: '拉瓦尔品第',
        value: 'PAK_RWP',
      },
      {
        label: '木尔坦',
        value: 'PAK_MUX',
      },
      {
        label: '伊斯兰堡',
        value: 'PAK_ISB',
      },
    ],
  },
  {
    label: '巴拿马',
    value: 'PAN',
  },
  {
    label: '巴拉圭',
    value: 'PAR',
  },
  {
    label: '皮特凯恩',
    value: 'PCN',
  },
  {
    label: '菲律宾',
    value: 'PHI',
  },
  {
    label: '巴勒斯坦',
    value: 'PLE',
  },
  {
    label: '帕劳群岛',
    value: 'PLW',
  },
  {
    label: '巴布亚新几内亚',
    value: 'PNG',
    children: [
      {
        label: '北部',
        value: 'PNG_NO',
      },
      {
        label: '布干维尔',
        value: 'PNG_BV',
      },
      {
        label: '东部高地',
        value: 'PNG_EH',
      },
      {
        label: '东塞皮克',
        value: 'PNG_ES',
      },
      {
        label: '东新不列颠',
        value: 'PNG_EB',
      },
      {
        label: '恩加',
        value: 'PNG_EN',
      },
      {
        label: '海湾',
        value: 'PNG_GU',
      },
      {
        label: '马当',
        value: 'PNG_MD',
      },
      {
        label: '马努斯',
        value: 'PNG_MN',
      },
      {
        label: '米尔恩湾',
        value: 'PNG_MB',
      },
      {
        label: '莫尔兹比港',
        value: 'PNG_NC',
      },
      {
        label: '莫罗贝',
        value: 'PNG_MR',
      },
      {
        label: '南部高地',
        value: 'PNG_SH',
      },
      {
        label: '钦布',
        value: 'PNG_SI',
      },
      {
        label: '桑道恩',
        value: 'PNG_SA',
      },
      {
        label: '西部',
        value: 'PNG_WE',
      },
      {
        label: '西部高地',
        value: 'PNG_WH',
      },
      {
        label: '西新不列颠',
        value: 'PNG_WB',
      },
      {
        label: '新爱尔兰',
        value: 'PNG_NI',
      },
    ],
  },
  {
    label: '波兰',
    value: 'POL',
    children: [
      {
        label: '埃尔布隆格',
        value: 'POL_ELB',
      },
      {
        label: '奥尔什丁',
        value: 'POL_OLS',
      },
      {
        label: '奥斯特罗文卡',
        value: 'POL_OSS',
      },
      {
        label: '比得哥什',
        value: 'POL_BZG',
      },
      {
        label: '彼得库夫',
        value: 'POL_PIO',
      },
      {
        label: '比托姆',
        value: 'POL_BYT',
      },
      {
        label: '比亚瓦波德拉斯卡',
        value: 'POL_BAP',
      },
      {
        label: '比亚维斯托克',
        value: 'POL_BIA',
      },
      {
        label: '波莱',
        value: 'POL_OPO',
      },
      {
        label: '波兹南',
        value: 'POL_POZ',
      },
      {
        label: '达布罗瓦戈尼察',
        value: 'POL_DAB',
      },
      {
        label: '大波兰地区戈茹夫',
        value: 'POL_GOW',
      },
      {
        label: '弗罗茨瓦夫',
        value: 'POL_WRO',
      },
      {
        label: '弗沃茨瓦韦克',
        value: 'POL_WLO',
      },
      {
        label: '格但斯克',
        value: 'POL_GDN',
      },
      {
        label: '格丁尼亚',
        value: 'POL_GDY',
      },
      {
        label: '格利维采',
        value: 'POL_GWC',
      },
      {
        label: '格鲁琼兹',
        value: 'POL_GRU',
      },
      {
        label: '海乌姆',
        value: 'POL_CHO',
      },
      {
        label: '华沙',
        value: 'POL_WAW',
      },
      {
        label: '霍茹夫',
        value: 'POL_CHZ',
      },
      {
        label: '卡利什',
        value: 'POL_KAL',
      },
      {
        label: '卡托维兹',
        value: 'POL_KTW',
      },
      {
        label: '凯尔采',
        value: 'POL_KLC',
      },
      {
        label: '科宁',
        value: 'POL_KON',
      },
      {
        label: '科沙林',
        value: 'POL_OSZ',
      },
      {
        label: '克拉科夫',
        value: 'POL_KRK',
      },
      {
        label: '克罗斯诺',
        value: 'POL_KRO',
      },
      {
        label: '拉多姆',
        value: 'POL_RDM',
      },
      {
        label: '莱格尼察',
        value: 'POL_LEG',
      },
      {
        label: '莱什诺',
        value: 'POL_LEZ',
      },
      {
        label: '卢布林',
        value: 'POL_LUL',
      },
      {
        label: '鲁达',
        value: 'POL_RDS',
      },
      {
        label: '罗兹',
        value: 'POL_LOD',
      },
      {
        label: '绿山城',
        value: 'POL_IEG',
      },
      {
        label: '米什洛维采',
        value: 'POL_MYL',
      },
      {
        label: '皮瓦',
        value: 'POL_PIL',
      },
      {
        label: '普热梅希尔',
        value: 'POL_PRZ',
      },
      {
        label: '普沃茨克',
        value: 'POL_PLO',
      },
      {
        label: '切哈努夫',
        value: 'POL_CIE',
      },
      {
        label: '热舒夫',
        value: 'POL_RZE',
      },
      {
        label: '什切青',
        value: 'POL_SZZ',
      },
      {
        label: '斯凯尔涅维采',
        value: 'POL_SKI',
      },
      {
        label: '斯武普斯克',
        value: 'POL_SLP',
      },
      {
        label: '苏瓦乌基',
        value: 'POL_SWL',
      },
      {
        label: '索波特',
        value: 'POL_SOP',
      },
      {
        label: '索斯诺维茨',
        value: 'POL_SWC',
      },
      {
        label: '塔尔努夫',
        value: 'POL_TAR',
      },
      {
        label: '塔尔诺布热格',
        value: 'POL_QEP',
      },
      {
        label: '特切',
        value: 'POL_TYY',
      },
      {
        label: '托伦',
        value: 'POL_TOR',
      },
      {
        label: '瓦乌布日赫',
        value: 'POL_WZH',
      },
      {
        label: '沃姆扎',
        value: 'POL_QOY',
      },
      {
        label: '希米亚诺维采',
        value: 'POL_SOW',
      },
      {
        label: '希维诺乌伊希切',
        value: 'POL_SWI',
      },
      {
        label: '希维托赫洛维采',
        value: 'POL_SWT',
      },
      {
        label: '谢德尔采',
        value: 'POL_SDC',
      },
      {
        label: '谢拉兹',
        value: 'POL_SIR',
      },
      {
        label: '新松奇',
        value: 'POL_NOW',
      },
      {
        label: '雅沃兹诺',
        value: 'POL_JAW',
      },
      {
        label: '耶莱尼亚古拉',
        value: 'POL_JEG',
      },
      {
        label: '扎布热',
        value: 'POL_ZAB',
      },
      {
        label: '扎莫希奇',
        value: 'POL_ZAM',
      },
    ],
  },
  {
    label: '葡萄牙',
    value: 'POR',
  },
  {
    label: '朝鲜',
    value: 'PRK',
    children: [
      {
        label: '海州',
        value: 'PRK_HAE',
      },
      {
        label: '惠山',
        value: 'PRK_HYE',
      },
      {
        label: '江界',
        value: 'PRK_KAN',
      },
      {
        label: '开城',
        value: 'PRK_KSN',
      },
      {
        label: '罗先',
        value: 'PRK_NAS',
      },
      {
        label: '南浦',
        value: 'PRK_NAM',
      },
      {
        label: '平壤',
        value: 'PRK_FNJ',
      },
      {
        label: '清津',
        value: 'PRK_CHO',
      },
      {
        label: '沙里院',
        value: 'PRK_SAR',
      },
      {
        label: '咸兴',
        value: 'PRK_HAM',
      },
      {
        label: '新义州',
        value: 'PRK_SII',
      },
      {
        label: '元山',
        value: 'PRK_WON',
      },
    ],
  },
  {
    label: '波多黎各',
    value: 'PUR',
  },
  {
    label: '法属波利尼西亚',
    value: 'PYF',
  },
  {
    label: '卡塔尔',
    value: 'QAT',
    children: [
      {
        label: '北部',
        value: 'QAT_MS',
      },
      {
        label: '多哈',
        value: 'QAT_DW',
      },
      {
        label: '古韦里耶',
        value: 'QAT_GW',
      },
      {
        label: '豪尔',
        value: 'QAT_KR',
      },
      {
        label: '杰里扬拜特奈',
        value: 'QAT_JB',
      },
      {
        label: '赖扬',
        value: 'QAT_RN',
      },
      {
        label: '沃克拉',
        value: 'QAT_WK',
      },
      {
        label: '乌姆锡拉勒',
        value: 'QAT_UL',
      },
      {
        label: '朱迈利耶',
        value: 'QAT_JM',
      },
    ],
  },
  {
    label: '留尼旺岛',
    value: 'REU',
  },
  {
    label: '马绍尔群岛',
    value: 'RMI',
  },
  {
    label: '罗马尼亚',
    value: 'ROM',
  },
  {
    label: '南非',
    value: 'RSA',
  },
  {
    label: '俄罗斯',
    value: 'RUS',
    children: [
      {
        label: '阿巴坎',
        value: 'RUS_ABA',
      },
      {
        label: '阿尔汉格尔斯克',
        value: 'RUS_ARK',
      },
      {
        label: '阿金斯科耶',
        value: 'RUS_AGI',
      },
      {
        label: '阿纳德尔',
        value: 'RUS_DYR',
      },
      {
        label: '阿斯特拉罕',
        value: 'RUS_AST',
      },
      {
        label: '埃利斯塔',
        value: 'RUS_ESL',
      },
      {
        label: '奥廖尔',
        value: 'RUS_ORL',
      },
      {
        label: '奥伦堡',
        value: 'RUS_ORE',
      },
      {
        label: '巴尔瑙尔',
        value: 'RUS_BAX',
      },
      {
        label: '奔萨',
        value: 'RUS_PNZ',
      },
      {
        label: '彼得罗巴甫洛夫斯克',
        value: 'RUS_PKC',
      },
      {
        label: '彼得罗扎沃茨克',
        value: 'RUS_PES',
      },
      {
        label: '比罗比詹',
        value: 'RUS_BBZ',
      },
      {
        label: '别尔哥罗德',
        value: 'RUS_BEL',
      },
      {
        label: '伯力',
        value: 'RUS_COK',
      },
      {
        label: '布拉戈维申斯克',
        value: 'RUS_BQS',
      },
      {
        label: '布良斯克',
        value: 'RUS_BRY',
      },
      {
        label: '赤塔',
        value: 'RUS_CHI',
      },
      {
        label: '顿河畔罗斯托夫',
        value: 'RUS_ROS',
      },
      {
        label: '鄂木斯克',
        value: 'RUS_OMS',
      },
      {
        label: '伏尔加格勒',
        value: 'RUS_VOG',
      },
      {
        label: '弗拉基米尔',
        value: 'RUS_VMR',
      },
      {
        label: '弗拉季高加索',
        value: 'RUS_VLA',
      },
      {
        label: '戈尔诺－阿尔泰斯克',
        value: 'RUS_GOA',
      },
      {
        label: '格罗兹尼',
        value: 'RUS_GRV',
      },
      {
        label: '海参崴',
        value: 'RUS_VVO',
      },
      {
        label: '汉特－曼西斯克',
        value: 'RUS_KHM',
      },
      {
        label: '基洛夫',
        value: 'RUS_KIR',
      },
      {
        label: '加里宁格勒',
        value: 'RUS_KGD',
      },
      {
        label: '喀山',
        value: 'RUS_KZN',
      },
      {
        label: '卡卢加',
        value: 'RUS_KLF',
      },
      {
        label: '科斯特罗马',
        value: 'RUS_KOS',
      },
      {
        label: '克拉斯诺达尔',
        value: 'RUS_KRR',
      },
      {
        label: '克拉斯诺亚尔斯克',
        value: 'RUS_KYA',
      },
      {
        label: '克麦罗沃',
        value: 'RUS_KEM',
      },
      {
        label: '克孜勒',
        value: 'RUS_KYZ',
      },
      {
        label: '库德姆卡尔',
        value: 'RUS_KUD',
      },
      {
        label: '库尔干',
        value: 'RUS_KRO',
      },
      {
        label: '库尔斯克',
        value: 'RUS_URS',
      },
      {
        label: '利佩茨克',
        value: 'RUS_LIP',
      },
      {
        label: '梁赞',
        value: 'RUS_RYA',
      },
      {
        label: '马哈奇卡拉',
        value: 'RUS_MCX',
      },
      {
        label: '马加丹',
        value: 'RUS_MAG',
      },
      {
        label: '马加斯',
        value: 'RUS_IN',
      },
      {
        label: '迈科普',
        value: 'RUS_MAY',
      },
      {
        label: '摩尔曼斯克',
        value: 'RUS_MMK',
      },
      {
        label: '莫斯科',
        value: 'RUS_MOW',
      },
      {
        label: '纳尔奇克',
        value: 'RUS_NAL',
      },
      {
        label: '纳里扬马尔',
        value: 'RUS_NNM',
      },
      {
        label: '南萨哈林斯克',
        value: 'RUS_JSA',
      },
      {
        label: '诺夫哥罗德',
        value: 'RUS_VUS',
      },
      {
        label: '帕拉纳',
        value: 'RUS_PAL',
      },
      {
        label: '普斯科夫',
        value: 'RUS_PSK',
      },
      {
        label: '切博克萨雷',
        value: 'RUS_CSY',
      },
      {
        label: '切尔克斯克',
        value: 'RUS_CKS',
      },
      {
        label: '秋明',
        value: 'RUS_TYU',
      },
      {
        label: '萨拉托夫',
        value: 'RUS_SAR',
      },
      {
        label: '萨兰斯克',
        value: 'RUS_SKX',
      },
      {
        label: '萨列哈尔德',
        value: 'RUS_SLY',
      },
      {
        label: '萨马拉',
        value: 'RUS_SAM',
      },
      {
        label: '瑟克特夫卡尔',
        value: 'RUS_SCW',
      },
      {
        label: '圣彼得堡',
        value: 'RUS_SPE',
      },
      {
        label: '斯摩棱斯克',
        value: 'RUS_LNX',
      },
      {
        label: '斯塔夫罗波尔',
        value: 'RUS_STA',
      },
      {
        label: '坦波夫',
        value: 'RUS_TAM',
      },
      {
        label: '特维尔',
        value: 'RUS_TVE',
      },
      {
        label: '图拉',
        value: 'RUS_TUL',
      },
      {
        label: '托木斯克',
        value: 'RUS_TOM',
      },
      {
        label: '沃罗涅什',
        value: 'RUS_VOR',
      },
      {
        label: '沃洛格达',
        value: 'RUS_VLG',
      },
      {
        label: '乌法',
        value: 'RUS_UFA',
      },
      {
        label: '乌兰乌德',
        value: 'RUS_UUD',
      },
      {
        label: '乌里扬诺夫斯克',
        value: 'RUS_ULY',
      },
      {
        label: '乌斯季奥尔登斯基',
        value: 'RUS_UOB',
      },
      {
        label: '下诺夫哥罗德',
        value: 'RUS_GOJ',
      },
      {
        label: '新西伯利亚',
        value: 'RUS_NVS',
      },
      {
        label: '雅库茨克',
        value: 'RUS_JAK',
      },
      {
        label: '雅罗斯拉夫尔',
        value: 'RUS_JAR',
      },
      {
        label: '叶卡捷林堡',
        value: 'RUS_JEK',
      },
      {
        label: '伊尔库茨克',
        value: 'RUS_IKT',
      },
      {
        label: '伊热夫斯克',
        value: 'RUS_IJK',
      },
      {
        label: '伊万诺沃',
        value: 'RUS_IVO',
      },
      {
        label: '约什卡尔奥拉',
        value: 'RUS_YOL',
      },
    ],
  },
  {
    label: '卢旺达',
    value: 'RWA',
    children: [
      {
        label: '比温巴',
        value: 'RWA_BY',
      },
      {
        label: '布塔雷',
        value: 'RWA_BU',
      },
      {
        label: '恩延扎',
        value: 'RWA_NY',
      },
      {
        label: '基本古',
        value: 'RWA_KG',
      },
      {
        label: '基布耶',
        value: 'RWA_KY',
      },
      {
        label: '基加利-恩加利',
        value: 'RWA_KR',
      },
      {
        label: '基加利市',
        value: 'RWA_KV',
      },
      {
        label: '吉孔戈罗',
        value: 'RWA_GK',
      },
      {
        label: '吉塞尼',
        value: 'RWA_GS',
      },
      {
        label: '吉塔拉马',
        value: 'RWA_GT',
      },
      {
        label: '卡布加',
        value: 'RWA_KA',
      },
      {
        label: '卢瓦马加纳',
        value: 'RWA_RW',
      },
      {
        label: '鲁汉戈',
        value: 'RWA_RH',
      },
      {
        label: '鲁亨盖里',
        value: 'RWA_RU',
      },
      {
        label: '尚古古',
        value: 'RWA_CY',
      },
      {
        label: '乌姆塔拉',
        value: 'RWA_UM',
      },
    ],
  },
  {
    label: '萨摩亚',
    value: 'SAM',
  },
  {
    label: '塞尔维亚和黑山',
    value: 'SCG',
    children: [
      {
        label: '贝尔格莱德',
        value: 'SCG_BEG',
      },
      {
        label: '波德戈里察',
        value: 'SCG_POD',
      },
      {
        label: '克拉古涅瓦茨',
        value: 'SCG_KGV',
      },
      {
        label: '尼什',
        value: 'SCG_INI',
      },
      {
        label: '诺维萨德',
        value: 'SCG_NVS',
      },
      {
        label: '普里什蒂纳',
        value: 'SCG_PRN',
      },
      {
        label: '苏博蒂察',
        value: 'SCG_SUB',
      },
      {
        label: '泽蒙',
        value: 'SCG_ZEM',
      },
    ],
  },
  {
    label: '塞内加尔',
    value: 'SEN',
    children: [
      {
        label: '达喀尔',
        value: 'SEN_DA',
      },
      {
        label: '法蒂克',
        value: 'SEN_FA',
      },
      {
        label: '济金绍尔',
        value: 'SEN_ZI',
      },
      {
        label: '捷斯',
        value: 'SEN_TH',
      },
      {
        label: '久尔贝勒',
        value: 'SEN_DI',
      },
      {
        label: '考拉克',
        value: 'SEN_KA',
      },
      {
        label: '科尔达',
        value: 'SEN_KO',
      },
      {
        label: '卢加',
        value: 'SEN_LO',
      },
      {
        label: '马塔姆',
        value: 'SEN_MA',
      },
      {
        label: '圣路易',
        value: 'SEN_SL',
      },
      {
        label: '坦巴昆达',
        value: 'SEN_TA',
      },
    ],
  },
  {
    label: '塞舌尔',
    value: 'SEY',
  },
  {
    label: '新加坡',
    value: 'SGP',
  },
  {
    label: '南乔治亚和南桑德威奇群岛',
    value: 'SGS',
  },
  {
    label: '圣赫勒拿',
    value: 'SHN',
  },
  {
    label: '斯瓦尔巴和扬马廷',
    value: 'SJM',
  },
  {
    label: '圣基茨和尼维斯',
    value: 'SKN',
  },
  {
    label: '塞拉利昂',
    value: 'SLE',
    children: [
      {
        label: '北部',
        value: 'SLE_N',
      },
      {
        label: '东部',
        value: 'SLE_E',
      },
      {
        label: '南部',
        value: 'SLE_S',
      },
      {
        label: '西部区',
        value: 'SLE_W',
      },
    ],
  },
  {
    label: '斯洛文尼亚',
    value: 'SLO',
  },
  {
    label: '圣马力诺',
    value: 'SMR',
  },
  {
    label: '所罗门群岛',
    value: 'SOL',
  },
  {
    label: '索马里',
    value: 'SOM',
  },
  {
    label: '圣皮埃尔和米克隆群岛',
    value: 'SPM',
  },
  {
    label: '塞尔维亚',
    value: 'SRB',
  },
  {
    label: '斯里兰卡',
    value: 'SRI',
  },
  {
    label: '圣多美和普林西比',
    value: 'STP',
  },
  {
    label: '苏丹',
    value: 'SUD',
  },
  {
    label: '瑞士',
    value: 'SUI',
  },
  {
    label: '苏里南',
    value: 'SUR',
    children: [
      {
        label: '布罗科蓬多',
        value: 'SUR_BR',
      },
      {
        label: '科罗尼',
        value: 'SUR_CR',
      },
      {
        label: '科默韦讷',
        value: 'SUR_CM',
      },
      {
        label: '马罗韦讷',
        value: 'SUR_MA',
      },
      {
        label: '尼克里',
        value: 'SUR_NI',
      },
      {
        label: '帕拉',
        value: 'SUR_PA',
      },
      {
        label: '帕拉马里博',
        value: 'SUR_PM',
      },
      {
        label: '萨拉马卡',
        value: 'SUR_SA',
      },
      {
        label: '瓦尼卡',
        value: 'SUR_WA',
      },
      {
        label: '西帕里韦尼',
        value: 'SUR_SI',
      },
    ],
  },
  {
    label: '斯洛伐克',
    value: 'SVK',
    children: [
      {
        label: '班斯卡-比斯特里察',
        value: 'SVK_BBY',
      },
      {
        label: '布拉迪斯拉发',
        value: 'SVK_BTS',
      },
      {
        label: '科希策',
        value: 'SVK_KOR',
      },
      {
        label: '尼特拉',
        value: 'SVK_NRA',
      },
      {
        label: '普雷绍夫',
        value: 'SVK_POV',
      },
      {
        label: '日利纳',
        value: 'SVK_RIL',
      },
      {
        label: '特尔纳瓦',
        value: 'SVK_TNA',
      },
      {
        label: '特伦钦',
        value: 'SVK_TRE',
      },
    ],
  },
  {
    label: '瑞典',
    value: 'SWE',
    children: [
      {
        label: '北博滕',
        value: 'SWE_BD',
      },
      {
        label: '布莱金厄',
        value: 'SWE_K',
      },
      {
        label: '达拉纳',
        value: 'SWE_DLN',
      },
      {
        label: '东约特兰',
        value: 'SWE_UGL',
      },
      {
        label: '厄勒布鲁',
        value: 'SWE_T',
      },
      {
        label: '哥得兰',
        value: 'SWE_I',
      },
      {
        label: '哈兰',
        value: 'SWE_N',
      },
      {
        label: '卡尔马',
        value: 'SWE_H',
      },
      {
        label: '克鲁努贝里',
        value: 'SWE_G',
      },
      {
        label: '南曼兰',
        value: 'SWE_D',
      },
      {
        label: '斯德哥尔摩',
        value: 'SWE_AB',
      },
      {
        label: '斯科耐',
        value: 'SWE_M',
      },
      {
        label: '韦姆兰',
        value: 'SWE_S',
      },
      {
        label: '乌普萨拉',
        value: 'SWE_C',
      },
      {
        label: '西博滕',
        value: 'SWE_AC',
      },
      {
        label: '西曼兰',
        value: 'SWE_U',
      },
      {
        label: '西诺尔兰',
        value: 'SWE_Y',
      },
      {
        label: '西约特兰',
        value: 'SWE_O',
      },
      {
        label: '延雪平',
        value: 'SWE_F',
      },
      {
        label: '耶夫勒堡',
        value: 'SWE_X',
      },
      {
        label: '耶姆特兰',
        value: 'SWE_Z',
      },
    ],
  },
  {
    label: '斯威士兰',
    value: 'SWZ',
  },
  {
    label: '叙利亚',
    value: 'SYR',
    children: [
      {
        label: '阿勒颇',
        value: 'SYR_HL',
      },
      {
        label: '大马士革',
        value: 'SYR_RD',
      },
      {
        label: '大马士革市',
        value: 'SYR_DI',
      },
      {
        label: '代尔祖尔',
        value: 'SYR_DZ',
      },
      {
        label: '德拉',
        value: 'SYR_DA',
      },
      {
        label: '哈马',
        value: 'SYR_HM',
      },
      {
        label: '哈塞克',
        value: 'SYR_HA',
      },
      {
        label: '霍姆斯',
        value: 'SYR_HI',
      },
      {
        label: '加布',
        value: 'SYR_GH',
      },
      {
        label: '卡米什利',
        value: 'SYR_QA',
      },
      {
        label: '库奈特拉',
        value: 'SYR_QU',
      },
      {
        label: '拉卡',
        value: 'SYR_RQ',
      },
      {
        label: '拉塔基亚',
        value: 'SYR_LA',
      },
      {
        label: '苏韦达',
        value: 'SYR_SU',
      },
      {
        label: '塔尔图斯',
        value: 'SYR_TA',
      },
      {
        label: '伊德利卜',
        value: 'SYR_ID',
      },
    ],
  },
  {
    label: '特里斯坦达昆哈',
    value: 'TAA',
  },
  {
    label: '坦桑尼亚',
    value: 'TAN',
  },
  {
    label: '特克斯和凯克特斯群岛',
    value: 'TCA',
  },
  {
    label: '汤加',
    value: 'TGA',
  },
  {
    label: '泰国',
    value: 'THA',
    children: [
      {
        label: '曼谷',
        value: 'THA_10',
      },
      {
        label: '北揽',
        value: 'THA_11',
      },
      {
        label: '暖武里',
        value: 'THA_12',
      },
      {
        label: '巴吞他尼',
        value: 'THA_13',
      },
      {
        label: '大城',
        value: 'THA_14',
      },
      {
        label: '红统',
        value: 'THA_15',
      },
      {
        label: '华富里',
        value: 'THA_16',
      },
      {
        label: '信武里',
        value: 'THA_17',
      },
      {
        label: '猜那',
        value: 'THA_18',
      },
      {
        label: '北标',
        value: 'THA_19',
      },
      {
        label: '春武里',
        value: 'THA_20',
      },
      {
        label: '拉农',
        value: 'THA_21',
      },
      {
        label: '尖竹汶',
        value: 'THA_22',
      },
      {
        label: '达叻',
        value: 'THA_23',
      },
      {
        label: '北柳',
        value: 'THA_24',
      },
      {
        label: '巴真',
        value: 'THA_25',
      },
      {
        label: '那空那育',
        value: 'THA_26',
      },
      {
        label: '沙缴',
        value: 'THA_27',
      },
      {
        label: '武里南',
        value: 'THA_31',
      },
      {
        label: '素林',
        value: 'THA_32',
      },
      {
        label: '四色菊',
        value: 'THA_33',
      },
      {
        label: '乌汶',
        value: 'THA_34',
      },
      {
        label: '耶梭通',
        value: 'THA_35',
      },
      {
        label: '猜也奔',
        value: 'THA_36',
      },
      {
        label: '安纳乍能',
        value: 'THA_37',
      },
      {
        label: '廊莫那浦',
        value: 'THA_39',
      },
      {
        label: '孔敬',
        value: 'THA_40',
      },
      {
        label: '乌隆',
        value: 'THA_41',
      },
      {
        label: '黎',
        value: 'THA_42',
      },
      {
        label: '廊开',
        value: 'THA_43',
      },
      {
        label: '玛哈沙拉堪',
        value: 'THA_44',
      },
      {
        label: '黎逸',
        value: 'THA_45',
      },
      {
        label: '加拉信',
        value: 'THA_46',
      },
      {
        label: '色军',
        value: 'THA_47',
      },
      {
        label: '那空帕农',
        value: 'THA_48',
      },
      {
        label: '莫达汉',
        value: 'THA_49',
      },
      {
        label: '清迈',
        value: 'THA_50',
      },
      {
        label: '南奔',
        value: 'THA_51',
      },
      {
        label: '程逸',
        value: 'THA_53',
      },
      {
        label: '帕',
        value: 'THA_54',
      },
      {
        label: '难',
        value: 'THA_55',
      },
      {
        label: '帕尧',
        value: 'THA_56',
      },
      {
        label: '清莱',
        value: 'THA_57',
      },
      {
        label: '夜丰颂',
        value: 'THA_58',
      },
      {
        label: '北榄坡',
        value: 'THA_60',
      },
      {
        label: '乌泰他尼',
        value: 'THA_61',
      },
      {
        label: '甘烹碧',
        value: 'THA_62',
      },
      {
        label: '达',
        value: 'THA_63',
      },
      {
        label: '素可泰',
        value: 'THA_64',
      },
      {
        label: '彭世洛',
        value: 'THA_65',
      },
      {
        label: '披集',
        value: 'THA_66',
      },
      {
        label: '叻丕',
        value: 'THA_70',
      },
      {
        label: '北碧',
        value: 'THA_71',
      },
      {
        label: '素攀武里',
        value: 'THA_72',
      },
      {
        label: '佛统',
        value: 'THA_73',
      },
      {
        label: '龙仔厝',
        value: 'THA_74',
      },
      {
        label: '夜功',
        value: 'THA_75',
      },
      {
        label: '碧差汶',
        value: 'THA_76',
      },
      {
        label: '巴蜀',
        value: 'THA_77',
      },
      {
        label: '佛丕',
        value: 'THA_78',
      },
      {
        label: '洛坤',
        value: 'THA_80',
      },
      {
        label: '甲米',
        value: 'THA_81',
      },
      {
        label: '攀牙',
        value: 'THA_82',
      },
      {
        label: '普吉',
        value: 'THA_83',
      },
      {
        label: '素叻',
        value: 'THA_84',
      },
      {
        label: '罗勇',
        value: 'THA_85',
      },
      {
        label: '春蓬',
        value: 'THA_86',
      },
      {
        label: '宋卡',
        value: 'THA_90',
      },
      {
        label: '沙敦',
        value: 'THA_91',
      },
      {
        label: '董里',
        value: 'THA_92',
      },
      {
        label: '博达伦',
        value: 'THA_93',
      },
      {
        label: '北大年',
        value: 'THA_94',
      },
      {
        label: '也拉',
        value: 'THA_95',
      },
      {
        label: '陶公',
        value: 'THA_96',
      },
    ],
  },
  {
    label: '塔吉克斯坦',
    value: 'TJK',
    children: [
      {
        label: '杜尚别',
        value: 'TJK_DYU',
      },
      {
        label: '霍罗格',
        value: 'TJK_KHO',
      },
      {
        label: '卡尼巴达姆',
        value: 'TJK_KAN',
      },
      {
        label: '科法尔尼洪',
        value: 'TJK_KOF',
      },
      {
        label: '苦盏',
        value: 'TJK_KHU',
      },
      {
        label: '库尔干-秋别',
        value: 'TJK_KTJ',
      },
      {
        label: '库洛布',
        value: 'TJK_KLB',
      },
      {
        label: '洛贡',
        value: 'TJK_RGU',
      },
      {
        label: '努雷克',
        value: 'TJK_NUR',
      },
      {
        label: '彭吉肯特',
        value: 'TJK_PJK',
      },
      {
        label: '萨班特',
        value: 'TJK_SBA',
      },
      {
        label: '塔博沙尔',
        value: 'TJK_TBS',
      },
      {
        label: '图尔孙扎德',
        value: 'TJK_TSZ',
      },
      {
        label: '乌拉秋别',
        value: 'TJK_UTJ',
      },
      {
        label: '伊斯法拉',
        value: 'TJK_ISF',
      },
    ],
  },
  {
    label: '托克劳',
    value: 'TKL',
  },
  {
    label: '土库曼斯坦',
    value: 'TKM',
    children: [
      {
        label: '阿哈尔',
        value: 'TKM_A',
      },
      {
        label: '阿什哈巴德市',
        value: 'TKM_ASB',
      },
      {
        label: '巴尔坎',
        value: 'TKM_B',
      },
      {
        label: '达沙古兹',
        value: 'TKM_D',
      },
      {
        label: '列巴普',
        value: 'TKM_L',
      },
      {
        label: '马雷',
        value: 'TKM_M',
      },
      {
        label: '涅比特达格',
        value: 'TKM_NEB',
      },
    ],
  },
  {
    label: '东帝汶',
    value: 'TLS',
    children: [
      {
        label: '阿伊莱乌',
        value: 'TLS_AL',
      },
      {
        label: '阿伊纳罗',
        value: 'TLS_AN',
      },
      {
        label: '埃尔梅拉',
        value: 'TLS_ER',
      },
      {
        label: '安贝诺',
        value: 'TLS_AM',
      },
      {
        label: '包考',
        value: 'TLS_BA',
      },
      {
        label: '博博纳罗',
        value: 'TLS_BO',
      },
      {
        label: '帝力',
        value: 'TLS_DI',
      },
      {
        label: '科瓦利马',
        value: 'TLS_KO',
      },
      {
        label: '劳滕',
        value: 'TLS_LA',
      },
      {
        label: '利基卡',
        value: 'TLS_LI',
      },
      {
        label: '马纳图托',
        value: 'TLS_MT',
      },
      {
        label: '马努法伊',
        value: 'TLS_MF',
      },
      {
        label: '维克克',
        value: 'TLS_VI',
      },
    ],
  },
  {
    label: '多哥',
    value: 'TOG',
  },
  {
    label: '特立尼达和多巴哥',
    value: 'TRI',
  },
  {
    label: '土耳其',
    value: 'TUR',
    children: [
      {
        label: '阿达纳',
        value: 'TUR_ADA',
      },
      {
        label: '阿德亚曼',
        value: 'TUR_ADI',
      },
      {
        label: '阿尔达罕',
        value: 'TUR_ARD',
      },
      {
        label: '阿尔特温',
        value: 'TUR_ART',
      },
      {
        label: '阿菲永',
        value: 'TUR_AFY',
      },
      {
        label: '阿克萨赖',
        value: 'TUR_AKS',
      },
      {
        label: '阿勒',
        value: 'TUR_AGR',
      },
      {
        label: '阿马西亚',
        value: 'TUR_AMA',
      },
      {
        label: '埃迪尔内',
        value: 'TUR_EDI',
      },
      {
        label: '埃尔津詹',
        value: 'TUR_EZC',
      },
      {
        label: '埃尔祖鲁姆',
        value: 'TUR_EZR',
      },
      {
        label: '埃拉泽',
        value: 'TUR_ELA',
      },
      {
        label: '埃斯基谢希尔',
        value: 'TUR_ESK',
      },
      {
        label: '艾登',
        value: 'TUR_AYI',
      },
      {
        label: '安卡拉',
        value: 'TUR_ANK',
      },
      {
        label: '安塔利亚',
        value: 'TUR_ANT',
      },
      {
        label: '奥尔杜',
        value: 'TUR_ORD',
      },
      {
        label: '巴尔腾',
        value: 'TUR_BAR',
      },
      {
        label: '巴勒克埃西尔',
        value: 'TUR_BAL',
      },
      {
        label: '巴特曼',
        value: 'TUR_BAT',
      },
      {
        label: '巴伊布尔特',
        value: 'TUR_BAY',
      },
      {
        label: '比莱吉克',
        value: 'TUR_BIL',
      },
      {
        label: '比特利斯',
        value: 'TUR_BIT',
      },
      {
        label: '宾格尔',
        value: 'TUR_BIN',
      },
      {
        label: '博卢',
        value: 'TUR_BOL',
      },
      {
        label: '布尔杜尔',
        value: 'TUR_BRD',
      },
      {
        label: '布尔萨',
        value: 'TUR_BRS',
      },
      {
        label: '昌克勒',
        value: 'TUR_CKR',
      },
      {
        label: '代尼兹利',
        value: 'TUR_DEN',
      },
      {
        label: '迪亚巴克尔',
        value: 'TUR_DIY',
      },
      {
        label: '凡',
        value: 'TUR_VAN',
      },
      {
        label: '哈卡里',
        value: 'TUR_HKR',
      },
      {
        label: '哈塔伊',
        value: 'TUR_HTY',
      },
      {
        label: '基利斯',
        value: 'TUR_KLS',
      },
      {
        label: '吉雷松',
        value: 'TUR_GIR',
      },
      {
        label: '加济安泰普',
        value: 'TUR_GAZ',
      },
      {
        label: '居米什哈内',
        value: 'TUR_GMS',
      },
      {
        label: '卡尔斯',
        value: 'TUR_KRS',
      },
      {
        label: '卡赫拉曼马拉什',
        value: 'TUR_KAH',
      },
      {
        label: '卡拉比克',
        value: 'TUR_KRB',
      },
      {
        label: '卡拉曼',
        value: 'TUR_KRM',
      },
      {
        label: '卡斯塔莫努',
        value: 'TUR_KAS',
      },
      {
        label: '开塞利',
        value: 'TUR_KAY',
      },
      {
        label: '科贾埃利',
        value: 'TUR_KOC',
      },
      {
        label: '柯克拉雷利',
        value: 'TUR_KLR',
      },
      {
        label: '科尼亚',
        value: 'TUR_KON',
      },
      {
        label: '克尔谢希尔',
        value: 'TUR_KRH',
      },
      {
        label: '克勒克卡莱',
        value: 'TUR_KRK',
      },
      {
        label: '拉飞',
        value: 'TUR_URF',
      },
      {
        label: '里泽',
        value: 'TUR_RIZ',
      },
      {
        label: '马尔丁',
        value: 'TUR_MAR',
      },
      {
        label: '马拉蒂亚',
        value: 'TUR_MAL',
      },
      {
        label: '马尼萨',
        value: 'TUR_MAN',
      },
      {
        label: '穆拉',
        value: 'TUR_MUG',
      },
      {
        label: '穆什',
        value: 'TUR_MUS',
      },
      {
        label: '内夫谢希尔',
        value: 'TUR_NEV',
      },
      {
        label: '尼代',
        value: 'TUR_NIG',
      },
      {
        label: '恰纳卡莱',
        value: 'TUR_CKL',
      },
      {
        label: '乔鲁姆',
        value: 'TUR_COR',
      },
      {
        label: '屈塔希亚',
        value: 'TUR_KUT',
      },
      {
        label: '萨卡里亚',
        value: 'TUR_SAK',
      },
      {
        label: '萨姆松',
        value: 'TUR_SAM',
      },
      {
        label: '泰基尔达',
        value: 'TUR_TEL',
      },
      {
        label: '特拉布宗',
        value: 'TUR_TRA',
      },
      {
        label: '托卡特',
        value: 'TUR_TOK',
      },
      {
        label: '乌萨克',
        value: 'TUR_USK',
      },
      {
        label: '锡尔纳克',
        value: 'TUR_SIR',
      },
      {
        label: '锡尔特',
        value: 'TUR_SII',
      },
      {
        label: '锡诺普',
        value: 'TUR_SIN',
      },
      {
        label: '锡瓦斯',
        value: 'TUR_SIV',
      },
      {
        label: '伊迪尔',
        value: 'TUR_IGD',
      },
      {
        label: '伊切尔',
        value: 'TUR_ICE',
      },
      {
        label: '伊斯帕尔塔',
        value: 'TUR_ISP',
      },
      {
        label: '伊斯坦布尔',
        value: 'TUR_IST',
      },
      {
        label: '伊兹密尔',
        value: 'TUR_IZM',
      },
      {
        label: '约兹加特',
        value: 'TUR_YOZ',
      },
      {
        label: '宗古尔达克',
        value: 'TUR_ZON',
      },
    ],
  },
  {
    label: '图瓦卢',
    value: 'TUV',
  },
  {
    label: '乌干达',
    value: 'UGA',
    children: [
      {
        label: '阿鲁阿',
        value: 'UGA_ARU',
      },
      {
        label: '阿帕克',
        value: 'UGA_APC',
      },
      {
        label: '阿朱马尼',
        value: 'UGA_ADJ',
      },
      {
        label: '本迪布焦',
        value: 'UGA_BUN',
      },
      {
        label: '布吉里',
        value: 'UGA_BUG',
      },
      {
        label: '布西亚',
        value: 'UGA_BUS',
      },
      {
        label: '布谢尼',
        value: 'UGA_BSH',
      },
      {
        label: '恩通加莫',
        value: 'UGA_NTU',
      },
      {
        label: '古卢',
        value: 'UGA_GUL',
      },
      {
        label: '霍伊马',
        value: 'UGA_HOI',
      },
      {
        label: '基巴莱',
        value: 'UGA_KBA',
      },
      {
        label: '基博加',
        value: 'UGA_KIB',
      },
      {
        label: '基恩乔乔',
        value: 'UGA_KYE',
      },
      {
        label: '基索罗',
        value: 'UGA_KIS',
      },
      {
        label: '基特古姆',
        value: 'UGA_KIT',
      },
      {
        label: '金贾',
        value: 'UGA_JIN',
      },
      {
        label: '卡巴莱',
        value: 'UGA_KBL',
      },
      {
        label: '卡巴罗莱',
        value: 'UGA_KAR',
      },
      {
        label: '卡贝拉马伊多',
        value: 'UGA_KAB',
      },
      {
        label: '卡兰加拉',
        value: 'UGA_KAL',
      },
      {
        label: '卡姆文盖',
        value: 'UGA_KAM',
      },
      {
        label: '卡穆利',
        value: 'UGA_KML',
      },
      {
        label: '卡农古',
        value: 'UGA_KAN',
      },
      {
        label: '卡普乔鲁瓦',
        value: 'UGA_KPC',
      },
      {
        label: '卡塞塞',
        value: 'UGA_KAS',
      },
      {
        label: '卡塔奎',
        value: 'UGA_KTK',
      },
      {
        label: '卡永加',
        value: 'UGA_KAY',
      },
      {
        label: '坎帕拉',
        value: 'UGA_KMP',
      },
      {
        label: '科蒂多',
        value: 'UGA_KOT',
      },
      {
        label: '库米',
        value: 'UGA_KUM',
      },
      {
        label: '拉卡伊',
        value: 'UGA_RAK',
      },
      {
        label: '利拉',
        value: 'UGA_LIR',
      },
      {
        label: '卢韦罗',
        value: 'UGA_LUW',
      },
      {
        label: '鲁昆吉里',
        value: 'UGA_RUK',
      },
      {
        label: '马萨卡',
        value: 'UGA_MAS',
      },
      {
        label: '马辛迪',
        value: 'UGA_MSN',
      },
      {
        label: '马尤盖',
        value: 'UGA_MAY',
      },
      {
        label: '莫罗托',
        value: 'UGA_MRT',
      },
      {
        label: '莫约',
        value: 'UGA_MOY',
      },
      {
        label: '姆巴拉拉',
        value: 'UGA_MBR',
      },
      {
        label: '姆巴莱',
        value: 'UGA_MBA',
      },
      {
        label: '姆皮吉',
        value: 'UGA_MPI',
      },
      {
        label: '穆本德',
        value: 'UGA_MUB',
      },
      {
        label: '穆科诺',
        value: 'UGA_MUK',
      },
      {
        label: '纳卡皮里皮里特',
        value: 'UGA_NAK',
      },
      {
        label: '纳卡松戈拉',
        value: 'UGA_NKS',
      },
      {
        label: '内比',
        value: 'UGA_NEB',
      },
      {
        label: '帕德尔',
        value: 'UGA_PAD',
      },
      {
        label: '帕利萨',
        value: 'UGA_PAL',
      },
      {
        label: '森巴布莱',
        value: 'UGA_SEM',
      },
      {
        label: '索罗提',
        value: 'UGA_SOR',
      },
      {
        label: '托罗罗',
        value: 'UGA_TOR',
      },
      {
        label: '瓦基索',
        value: 'UGA_WAK',
      },
      {
        label: '锡龙科',
        value: 'UGA_SIR',
      },
      {
        label: '伊甘加',
        value: 'UGA_IGA',
      },
      {
        label: '永贝',
        value: 'UGA_YUM',
      },
    ],
  },
  {
    label: '乌克兰',
    value: 'UKR',
    children: [
      {
        label: '文尼察',
        value: 'UKR_5',
      },
      {
        label: '沃伦',
        value: 'UKR_7',
      },
      {
        label: '卢甘斯克',
        value: 'UKR_9',
      },
      {
        label: '第聂伯罗波得罗夫斯克',
        value: 'UKR_12',
      },
      {
        label: '顿涅茨克',
        value: 'UKR_14',
      },
      {
        label: '日托米尔',
        value: 'UKR_18',
      },
      {
        label: '外喀尔巴阡',
        value: 'UKR_21',
      },
      {
        label: '扎波罗热',
        value: 'UKR_23',
      },
      {
        label: '伊万－弗兰科夫州',
        value: 'UKR_26',
      },
      {
        label: '基辅',
        value: 'UKR_30',
      },
      {
        label: '基洛夫格勒',
        value: 'UKR_35',
      },
      {
        label: '克里米亚自治共和国',
        value: 'UKR_43',
      },
      {
        label: '利沃夫',
        value: 'UKR_46',
      },
      {
        label: '尼古拉耶夫',
        value: 'UKR_48',
      },
      {
        label: '敖德萨',
        value: 'UKR_51',
      },
      {
        label: '波尔塔瓦',
        value: 'UKR_53',
      },
      {
        label: '罗夫诺',
        value: 'UKR_56',
      },
      {
        label: '苏梅',
        value: 'UKR_59',
      },
      {
        label: '捷尔诺波尔',
        value: 'UKR_61',
      },
      {
        label: '哈尔科夫',
        value: 'UKR_63',
      },
      {
        label: '赫尔松州',
        value: 'UKR_65',
      },
      {
        label: '赫梅利尼茨基',
        value: 'UKR_68',
      },
      {
        label: '切尔卡瑟',
        value: 'UKR_71',
      },
      {
        label: '切尔尼戈夫',
        value: 'UKR_74',
      },
      {
        label: '切尔诺夫策',
        value: 'UKR_77',
      },
    ],
  },
  {
    label: '美属外岛',
    value: 'UMI',
  },
  {
    label: '乌拉圭',
    value: 'URU',
  },
  {
    label: '美国',
    value: 'USA',
    children: [
      {
        label: '阿肯色',
        value: 'USA_AR',
      },
      {
        label: '阿拉巴马',
        value: 'USA_AL',
      },
      {
        label: '阿拉斯加',
        value: 'USA_AK',
      },
      {
        label: '爱达荷',
        value: 'USA_ID',
      },
      {
        label: '爱荷华',
        value: 'USA_IA',
      },
      {
        label: '北达科他',
        value: 'USA_ND',
      },
      {
        label: '北卡罗来纳',
        value: 'USA_NC',
      },
      {
        label: '宾夕法尼亚',
        value: 'USA_PA',
      },
      {
        label: '德克萨斯',
        value: 'USA_TX',
      },
      {
        label: '俄亥俄',
        value: 'USA_OH',
      },
      {
        label: '俄克拉荷马',
        value: 'USA_OK',
      },
      {
        label: '俄勒冈',
        value: 'USA_OR',
      },
      {
        label: '佛罗里达',
        value: 'USA_FL',
      },
      {
        label: '佛蒙特',
        value: 'USA_VT',
      },
      {
        label: '哥伦比亚特区',
        value: 'USA_DC',
      },
      {
        label: '华盛顿',
        value: 'USA_WA',
      },
      {
        label: '怀俄明',
        value: 'USA_WY',
      },
      {
        label: '加利福尼亚',
        value: 'USA_CA',
      },
      {
        label: '堪萨斯',
        value: 'USA_KS',
      },
      {
        label: '康涅狄格',
        value: 'USA_CT',
      },
      {
        label: '科罗拉多',
        value: 'USA_CO',
      },
      {
        label: '肯塔基',
        value: 'USA_KY',
      },
      {
        label: '路易斯安那',
        value: 'USA_LA',
      },
      {
        label: '罗德岛',
        value: 'USA_RI',
      },
      {
        label: '马里兰',
        value: 'USA_MD',
      },
      {
        label: '马萨诸塞',
        value: 'USA_MA',
      },
      {
        label: '蒙大拿',
        value: 'USA_MT',
      },
      {
        label: '密苏里',
        value: 'USA_MO',
      },
      {
        label: '密西西比',
        value: 'USA_MS',
      },
      {
        label: '密歇根',
        value: 'USA_MI',
      },
      {
        label: '缅因',
        value: 'USA_ME',
      },
      {
        label: '明尼苏达',
        value: 'USA_MN',
      },
      {
        label: '南达科他',
        value: 'USA_SD',
      },
      {
        label: '南卡罗来纳',
        value: 'USA_SC',
      },
      {
        label: '内布拉斯加',
        value: 'USA_NE',
      },
      {
        label: '内华达',
        value: 'USA_NV',
      },
      {
        label: '纽约',
        value: 'USA_NY',
      },
      {
        label: '特拉华',
        value: 'USA_DE',
      },
      {
        label: '田纳西',
        value: 'USA_TN',
      },
      {
        label: '威斯康星',
        value: 'USA_WI',
      },
      {
        label: '维吉尼亚',
        value: 'USA_VA',
      },
      {
        label: '西佛吉尼亚',
        value: 'USA_WV',
      },
      {
        label: '夏威夷',
        value: 'USA_HI',
      },
      {
        label: '新罕布什尔',
        value: 'USA_NH',
      },
      {
        label: '新墨西哥',
        value: 'USA_NM',
      },
      {
        label: '新泽西',
        value: 'USA_NJ',
      },
      {
        label: '亚利桑那',
        value: 'USA_AZ',
      },
      {
        label: '伊利诺斯',
        value: 'USA_IL',
      },
      {
        label: '印第安那',
        value: 'USA_IN',
      },
      {
        label: '犹他',
        value: 'USA_UT',
      },
      {
        label: '佐治亚',
        value: 'USA_GA',
      },
    ],
  },
  {
    label: '乌兹别克斯坦',
    value: 'UZB',
    children: [
      {
        label: '安集延',
        value: 'UZB_AN',
      },
      {
        label: '布哈拉',
        value: 'UZB_BU',
      },
      {
        label: '费尔干纳',
        value: 'UZB_FA',
      },
      {
        label: '花拉子模',
        value: 'UZB_XO',
      },
      {
        label: '吉扎克',
        value: 'UZB_JI',
      },
      {
        label: '卡拉卡尔帕克斯坦共和国',
        value: 'UZB_QR',
      },
      {
        label: '卡什卡达里亚',
        value: 'UZB_QA',
      },
      {
        label: '纳曼干',
        value: 'UZB_NG',
      },
      {
        label: '纳沃伊',
        value: 'UZB_NW',
      },
      {
        label: '撒马尔罕',
        value: 'UZB_SA',
      },
      {
        label: '苏尔汉河',
        value: 'UZB_SU',
      },
      {
        label: '塔什干',
        value: 'UZB_TK',
      },
      {
        label: '塔什干市',
        value: 'UZB_TO',
      },
      {
        label: '锡尔河',
        value: 'UZB_SI',
      },
    ],
  },
  {
    label: '瓦努阿图',
    value: 'VAN',
  },
  {
    label: '梵蒂冈',
    value: 'VAT',
  },
  {
    label: '委内瑞拉',
    value: 'VEN',
    children: [
      {
        label: '阿拉瓜',
        value: 'VEN_D',
      },
      {
        label: '阿马库罗三角洲',
        value: 'VEN_Y',
      },
      {
        label: '阿普雷',
        value: 'VEN_C',
      },
      {
        label: '安索阿特吉',
        value: 'VEN_B',
      },
      {
        label: '巴里纳斯',
        value: 'VEN_E',
      },
      {
        label: '玻利瓦尔',
        value: 'VEN_F',
      },
      {
        label: '波图格萨',
        value: 'VEN_P',
      },
      {
        label: '法尔孔',
        value: 'VEN_I',
      },
      {
        label: '瓜里科',
        value: 'VEN_J',
      },
      {
        label: '加拉加斯',
        value: 'VEN_A',
      },
      {
        label: '卡拉沃沃',
        value: 'VEN_G',
      },
      {
        label: '科赫德斯',
        value: 'VEN_H',
      },
      {
        label: '拉腊',
        value: 'VEN_K',
      },
      {
        label: '联邦属地',
        value: 'VEN_W',
      },
      {
        label: '梅里达',
        value: 'VEN_L',
      },
      {
        label: '米兰达',
        value: 'VEN_M',
      },
      {
        label: '莫纳加斯',
        value: 'VEN_N',
      },
      {
        label: '苏克雷',
        value: 'VEN_R',
      },
      {
        label: '苏利亚',
        value: 'VEN_V',
      },
      {
        label: '塔奇拉',
        value: 'VEN_S',
      },
      {
        label: '特鲁希略',
        value: 'VEN_T',
      },
      {
        label: '新埃斯帕塔',
        value: 'VEN_O',
      },
      {
        label: '亚拉奎',
        value: 'VEN_U',
      },
      {
        label: '亚马孙',
        value: 'VEN_Z',
      },
    ],
  },
  {
    label: '维尔京群岛',
    value: 'VGB',
  },
  {
    label: '越南',
    value: 'VIE',
  },
  {
    label: '圣文森特和格林纳丁斯',
    value: 'VIN',
  },
  {
    label: '维尔京群岛',
    value: 'VIR',
  },
  {
    label: '瓦利斯和福图纳',
    value: 'WLF',
  },
  {
    label: '也门',
    value: 'YEM',
    children: [
      {
        label: '阿比扬',
        value: 'YEM_AB',
      },
      {
        label: '阿姆兰',
        value: 'YEM_AM',
      },
      {
        label: '贝达',
        value: 'YEM_BA',
      },
      {
        label: '达利',
        value: 'YEM_DA',
      },
      {
        label: '哈德拉毛',
        value: 'YEM_HD',
      },
      {
        label: '哈杰',
        value: 'YEM_HJ',
      },
      {
        label: '荷台达',
        value: 'YEM_HU',
      },
      {
        label: '焦夫',
        value: 'YEM_JA',
      },
      {
        label: '拉赫季',
        value: 'YEM_LA',
      },
      {
        label: '马里卜',
        value: 'YEM_MA',
      },
      {
        label: '迈赫拉',
        value: 'YEM_MR',
      },
      {
        label: '迈赫维特',
        value: 'YEM_MW',
      },
      {
        label: '萨达',
        value: 'YEM_SD',
      },
      {
        label: '萨那',
        value: 'YEM_SN',
      },
      {
        label: '赛文',
        value: 'YEM_GXF',
      },
      {
        label: '舍卜沃',
        value: 'YEM_SH',
      },
      {
        label: '塔伊兹',
        value: 'YEM_TA',
      },
      {
        label: '希赫尔',
        value: 'YEM_ASR',
      },
      {
        label: '亚丁',
        value: 'YEM_AD',
      },
      {
        label: '伊卜',
        value: 'YEM_IB',
      },
      {
        label: '扎玛尔',
        value: 'YEM_DH',
      },
    ],
  },
  {
    label: '赞比亚',
    value: 'ZAM',
  },
  {
    label: '津巴布韦',
    value: 'ZIM',
  },
];

export function getCountriesByLevel(level?: 'C' | 'P' | 'PC' | 'PCD' | 'detail') {
  switch (level) {
    case 'C':
      // 仅国家层级：去掉所有 children
      return [CHINA_P, ...COUNTRIES_TREE].map(({ label, value }) => ({ label, value }));

    case 'P':
      // 国家 - 省：中国用 CHINA_P，其他国家保留自身结构
      return [CHINA_P, ...COUNTRIES_TREE];

    case 'PC':
      // 国家 - 省 - 市
      return [CHINA_PC, ...COUNTRIES_TREE];

    case 'PCD':
    default:
      // 国家 - 省 - 市 - 区
      return [CHINA_PCD, ...COUNTRIES_TREE];
  }
}
