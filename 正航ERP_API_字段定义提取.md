# 正航ERP API接口文档 - 字段定义提取

> 来源文件：正航ERPAPI接口文档V2.2.html
> 提取范围：认证接口、通用调用方式、comMaterialGroup、comMaterial、ppBOM、purReceivingOrder_RC、ppProduceOrder

---

## 1. 认证接口 (auth)

### 调用方式

| 项目 | 值 |
|------|------|
| 接口地址 | `http://(ESB服务地址及端口)/esb/api/auth.do` |
| 请求方式 | HTTP POST |
| 请求格式 | JSON (Content-Type: application/json) |
| 响应格式 | JSON (Content-Type: application/json) |
| 编码格式 | UTF-8 |

### 请求参数

| 参数名 | 类型 | 必填 | 备注 |
|--------|------|------|------|
| appid | String | Y | appid，由正航提供 |
| time | String/Long | Y | 发送请求时的utc时间戳，精确到毫秒，再加上6位随机数 |
| sign | String | Y | 签名，**appid+appsecret+time**进行md5加密 |
| language | String | - | 指定认证时使用的语言，非必填，为空时默认取ESB认证设定的语言。选项：en<English> / zh-CHS<中文(简体)> / zh-CHT<中文(繁體)> |

### 请求参数示例

```json
{
    "appid": "chiesb185a1fbdc22781f1",
    "time": "1718855090958191519",
    "sign": "CFCCF02D2D1370757F295472A2F597D1",
    "language": "zh-CHS"
}
```

### sign生成案例

- appid: `chiesb185a1fbdc22781f1`（由正航提供）
- appsecret: `2BB2D1D899C32BF091BEC8DDA9EBCB72`（由正航提供）
- 当前utc时间 `2024-06-20 11:44:50` 对应时间戳（毫秒）为 `1718855090958`
- 加上6位随机数 `191519`，得到 time = `1718855090958191519`
- appid+appsecret+time = `chiesb185a1fbdc22781f12BB2D1D899C32BF091BEC8DDA9EBCB721718855090958191519`
- 对该字符串MD5得到 sign = `CFCCF02D2D1370757F295472A2F597D1`（注意需要大写）

### 响应参数 - 成功

| 参数名 | 类型 | 长度 | 备注 |
|--------|------|------|------|
| status | Int | - | 执行结果，成功为1 |
| token | String | 32 | 身份令牌 |
| timeout | Long | - | token的有效期（秒） |
| time | Long | - | 服务端响应请求时的utc时间 |
| sign | String | 32 | 签名，**appid+appsecret+token+time**进行md5加密 |
| trcode | Long | - | 内部事务号 |
| cltrcode | Long | - | 请求参数中的事务号，返回参数中返回，用于标识这一次请求 |
| etime | Long | 14 | 服务的执行开始时间（UTC） |
| eduration | Long | - | 服务的执行时长（Ticks） |

### 成功响应示例

```json
{
    "status": 1,
    "token": "EF2F73E11901DE6FBB5AA278EC17D089",
    "timeout": 7200,
    "time": 17188872171429,
    "sign": "BEC83CFD1AD6F2C83A25BAADDACE3463",
    "trcode": 17742,
    "cltrcode": 0,
    "etime": 20240620123923,
    "eduration": 27363476
}
```

### 响应参数 - 失败

| 参数名 | 类型 | 长度 | 备注 |
|--------|------|------|------|
| status | Int | - | 执行结果，失败为0 |
| error | String | - | 错误信息 |
| trcode | Long | - | 内部事务号 |
| cltrcode | Long | - | 请求参数中的事务号，返回参数中返回，用于标识这一次请求 |
| etime | Long | 14 | 服务的执行开始时间（UTC） |
| eduration | Long | - | 服务的执行时长（Ticks） |

### 失败响应示例

```json
{
    "status": 0,
    "error": "验证AppSecret失败",
    "trcode": 17736,
    "cltrcode": 0,
    "etime": 20240620122240,
    "eduration": 19590115
}
```

---

## 2. 通用调用方式

所有业务API（新增/清单查询/明细查询）共用以下调用格式：

| 项目 | 值 |
|------|------|
| 请求方式 | HTTP POST |
| 请求格式 | JSON (Content-Type: application/json) |
| 响应格式 | JSON (Content-Type: application/json) |
| 编码格式 | UTF-8 |

### 接口地址分类

| 操作类型 | 接口地址 | 说明 |
|----------|----------|------|
| 新增 | `/esb/erp/addnew.do` | 新增业务数据 |
| 清单查询 (getlist) | `/esb/erp/getlist.do` | 分页查询清单数据 |
| 明细查询 (getdata) | `/esb/erp/get.do` | 查询单据明细数据 |

### getlist 通用请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| token | String | Y | 经过ESB认证得到的token |
| time | String/Long | Y | 发送请求时的utc时间戳，精确到毫秒，再加上一位随机数 |
| sign | String | Y | 签名（**token+appsecret+time**进行md5加密） |
| progid | String | Y | 功能编号（如 comMaterialGroup / comMaterial / ppBOM 等） |
| data | Object | Y | 用于指定本次查询的条件 |
| data.condition | String | - | 符合SQL语法的Where条件表达式 |
| data.lastpkvalues | String | - | 指定从哪笔主键开始查找 |

### getdata 通用请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| token | String | Y | 经过ESB认证得到的token |
| time | String/Long | Y | 发送请求时的utc时间戳，精确到毫秒，再加上一位随机数 |
| sign | String | Y | 签名（**token+appsecret+time**进行md5加密） |
| progid | String | Y | 功能编号 |
| data | Object | Y | 用于指定本次查询的条件 |
| data.pkvalues | String | Y | 主键值，多个主键用,分隔 |

### getlist 通用响应参数 - 成功

| 参数名 | 类型 | 长度 | 备注 |
|--------|------|------|------|
| data | Object | - | 返回结果（Json格式） |
| lastpkvalues | String | - | 最后一笔的主键 |
| hasnext | Boolean | - | 是否还有数据 |
| status | Int | - | 执行结果，成功为1 |
| trcode | Long | - | 内部事务号 |
| cltrcode | Long | - | 请求参数中的事务号，返回参数中返回，用于标识这一次请求 |
| etime | Long | 14 | 服务的执行开始时间（UTC） |
| eduration | Long | - | 服务的执行时长（Ticks） |

### getlist/getdata 通用响应参数 - 失败

| 参数名 | 类型 | 长度 | 备注 |
|--------|------|------|------|
| status | Int | - | 执行结果，失败为0 |
| error | String | 50 | 错误信息 |
| errorcode | String | - | 错误码 |
| trcode | Long | - | 内部事务号 |
| cltrcode | Long | - | 请求参数中的事务号，返回参数中返回，用于标识这一次请求 |
| etime | Long | 14 | 服务的执行开始时间（UTC） |
| eduration | Long | - | 服务的执行时长（Ticks） |

### getdata 通用响应参数 - 成功

| 参数名 | 类型 | 长度 | 备注 |
|--------|------|------|------|
| data | Object | - | 返回结果（Json格式） |
| status | Int | - | 执行结果，成功为1 |
| trcode | Long | - | 内部事务号 |
| cltrcode | Long | - | 请求参数中的事务号，返回参数中返回，用于标识这一次请求 |
| etime | Long | 14 | 服务的执行开始时间（UTC） |
| eduration | Long | - | 服务的执行时长（Ticks） |

### 失败响应示例（通用）

```json
{
    "status": 0,
    "error": "验证Token失败, Token错误或者已失效",
    "errorcode": "10001",
    "trcode": 10957,
    "cltrcode": 0,
    "etime": 20210707055312,
    "eduration": 21842114
}
```

---

## 3. comMaterialGroup（物料基础数据）- 清单查询

### 调用方式

| 项目 | 值 |
|------|------|
| 接口地址 | `/esb/erp/getlist.do` |
| progid | `comMaterialGroup` |

### 请求参数

（同 getlist 通用请求参数，progid 值为 `comMaterialGroup`）

### 请求参数示例

```json
{
    "token": "E543F9D3D4FA017BC02F9E4BBCEEE6AB",
    "time": "20211117015449",
    "sign": "A3FF17E66DF57507FF51EBCBE4B0C7E5",
    "progid": "comMaterialGroup",
    "data": {
        "condition": "MaterialId = ''",
        "lastpkvalues": ""
    }
}
```

### 数据字典 - MaterialGroup 表（9个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| MaterialId | 代码 | String | Y | Y | - | 40 | |
| MaterialName | 物料名称 | String | - | Y | - | 120 | |
| MaterialTypeId | 物料类型 | String | - | - | - | 10 | |
| MaterialSpec | 物料规格 | String | - | - | - | 200 | |
| MaterialCategoryId | 物料类别 | String | - | - | - | 20 | 需同ERP后台的物料类别 |
| UnitId | 基本计量单位 | String | - | - | - | 20 | |
| ValidityFromDate | 有效期从 | String | - | - | - | 10 | 格式要求:YYYY-MM-DD 举例2023-09-19 |
| ValidityToDate | 有效期至 | String | - | - | - | 10 | 格式要求:YYYY-MM-DD 举例2023-09-19 |
| UseDealMultiUnit | 使用交易多单位 | Boolean | - | - | - | 1 | |

### 响应参数

（同 getlist 通用响应参数）

### 成功响应示例

```json
{
    "data": {
        "MaterialGroup": [
            {
                "MaterialId": "",
                "MaterialName": "",
                "MaterialTypeId": "",
                "MaterialSpec": "",
                "MaterialCategoryId": "",
                "UnitId": "",
                "ValidityFromDate": "",
                "ValidityToDate": "",
                "UseDealMultiUnit": false
            }
        ]
    },
    "lastpkvalues": "",
    "hasnext": false,
    "status": 1,
    "trcode": 17765,
    "cltrcode": 0,
    "etime": 20240702132548,
    "eduration": 65603
}
```

---

## 4. comMaterial（物料公司数据）- 清单查询

### 调用方式

| 项目 | 值 |
|------|------|
| 接口地址 | `/esb/erp/getlist.do` |
| progid | `comMaterial` |

### 请求参数

（同 getlist 通用请求参数，progid 值为 `comMaterial`）

### 请求参数示例

```json
{
    "token": "E543F9D3D4FA017BC02F9E4BBCEEE6AB",
    "time": "20211117015449",
    "sign": "A3FF17E66DF57507FF51EBCBE4B0C7E5",
    "progid": "comMaterial",
    "data": {
        "condition": "FOrgId = ''",
        "lastpkvalues": ""
    }
}
```

### 数据字典 - companyMaterial 表（13个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| MaterialId | 物料代码 | String | Y | Y | - | 40 | |
| MaterialTypeId | 物料类型 | String | - | - | - | 10 | |
| MaterialCategoryId | 物料类别 | String | - | Y | - | 20 | 导入物料类别需要同ERP后台 |
| IsCalculateQty | 核算库存数量 | Boolean | - | - | - | 1 | |
| IsCalculateCost | 核算存货成本 | Boolean | - | - | - | 1 | |
| IsSalMat | 销售物料 | Boolean | - | - | - | 1 | |
| IsPurMat | 采购物料 | Boolean | - | - | - | 1 | |
| IsPlanMat | 计划物料 | Boolean | - | - | - | 1 | |
| IsProdMat | 生产物料 | Boolean | - | - | - | 1 | |
| IsQltyMat | 质量物料 | Boolean | - | - | - | 1 | |
| IsWebMat | 网购物料 | Boolean | - | - | - | 1 | |
| ValidityFromDate | 有效期从 | String | - | - | - | 10 | 格式要求:YYYY-MM-DD 举例2023-09-19 |
| ValidityToDate | 有效期至 | String | - | - | - | 10 | 格式要求:YYYY-MM-DD 举例2023-09-19 |

### 响应参数

（同 getlist 通用响应参数）

---

## 5. ppBOM（BOM）- 清单查询 + 明细查询

### 5.1 ppBOM getlist（清单查询）

#### 调用方式

| 项目 | 值 |
|------|------|
| 接口地址 | `/esb/erp/getlist.do` |
| progid | `ppBOM` |

#### 请求参数

（同 getlist 通用请求参数，progid 值为 `ppBOM`）

#### 请求参数示例

```json
{
    "token": "E543F9D3D4FA017BC02F9E4BBCEEE6AB",
    "time": "20211117015449",
    "sign": "A3FF17E66DF57507FF51EBCBE4B0C7E5",
    "progid": "ppBOM",
    "data": {
        "condition": "BOMKeyId = ''",
        "lastpkvalues": ""
    }
}
```

#### 数据字典 - BOMMainInfo 表（12个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| BOMKeyId | 代码 | String | Y | Y | - | 50 | |
| BOMKeyName | 名称 | String | - | Y | - | 120 | |
| MaterialId | 母件 | String | - | Y | - | 40 | 值同erp后台的物料代码 |
| BizAttr | BOM属性 | Byte | - | Y | - | 3 | 下拉选项：0标准BOM;1联产品BOM;2结构BOM;3虚拟BOM;4通用BOM;5订单BOM |
| Version | 版本号 | String | - | Y | - | 20 | |
| BOMStyleId | BOM类型 | String | - | Y | - | 10 | 固定传入值0001 |
| BOMTypeId | BOM用途类别 | String | - | - | - | 20 | 值同ERP后台BOM用途类别 |
| BOMSerNo | 序 | Int | - | - | - | 10 | |
| FromBizPartnerId | 客户代码 | String | - | - | - | 20 | |
| FromBillCategory | 来源单种类 | Byte | - | - | - | 3 | |
| FromBillNo | 来源单号 | String | - | - | - | 20 | |
| FromRowCode | 来源标识号 | Int | - | - | - | 10 | |

#### getlist 成功响应示例

```json
{
    "data": {
        "BOMMainInfo": [
            {
                "BOMKeyId": "",
                "BOMKeyName": "",
                "MaterialId": "",
                "BizAttr": 0,
                "Version": "",
                "BOMStyleId": "",
                "BOMTypeId": "",
                "BOMSerNo": 0,
                "FromBizPartnerId": "",
                "FromBillCategory": 0,
                "FromBillNo": "",
                "FromRowCode": 0
            }
        ]
    },
    "lastpkvalues": "",
    "hasnext": false,
    "status": 1,
    "trcode": 17765,
    "cltrcode": 0,
    "etime": 20240702132548,
    "eduration": 65603
}
```

### 5.2 ppBOM getdata（明细查询）

#### 调用方式

| 项目 | 值 |
|------|------|
| 接口地址 | `/esb/erp/get.do` |
| progid | `ppBOM` |

#### 请求参数

（同 getdata 通用请求参数，progid 值为 `ppBOM`）

#### 请求参数示例

```json
{
    "token": "E543F9D3D4FA017BC02F9E4BBCEEE6AB",
    "time": "20211117015449",
    "sign": "A3FF17E66DF57507FF51EBCBE4B0C7E5",
    "progid": "ppBOM",
    "data": {
        "pkvalues": ""
    }
}
```

#### 数据字典 - 共5个数据表

##### 表1: BOMMainInfo（BOM表头，12个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| BOMKeyId | 代码 | String | Y | Y | - | 50 | |
| BOMKeyName | 名称 | String | - | Y | - | 120 | |
| MaterialId | 母件 | String | - | Y | - | 40 | 值同erp后台的物料代码 |
| BizAttr | BOM属性 | Byte | - | Y | - | 3 | 下拉选项：0标准BOM;1联产品BOM;2结构BOM;3虚拟BOM;4通用BOM;5订单BOM |
| Version | 版本号 | String | - | Y | - | 20 | |
| BOMStyleId | BOM类型 | String | - | Y | - | 10 | 固定传入值0001 |
| BOMTypeId | BOM用途类别 | String | - | - | - | 20 | 值同ERP后台BOM用途类别 |
| BOMSerNo | 序 | Int | - | - | - | 10 | |
| FromBizPartnerId | 客户代码 | String | - | - | - | 20 | |
| FromBillCategory | 来源单种类 | Byte | - | - | - | 3 | |
| FromBillNo | 来源单号 | String | - | - | - | 20 | |
| FromRowCode | 来源标识号 | Int | - | - | - | 10 | |

##### 表2: BOMSubMatBatchQtyInfo（BOM子件批次数量信息，6个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| BOMKeyId | BOM代码 | String | Y | Y | - | 50 | |
| ParentRowCode | 父标识号 | Int | Y | Y | - | 10 | |
| RowCode | 标识号 | Int | Y | Y | - | 10 | |
| BeginBatchQty | 母件起始批量 | Decimal | - | Y | 9 | 19 | |
| BaseQty | 母件基数 | Decimal | - | Y | 9 | 19 | |
| UnitQty | 用量 | Decimal | - | Y | 9 | 19 | |

##### 表3: BOMSubMatInfo（BOM子件信息，6个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| BOMKeyId | BOM代码 | String | Y | Y | - | 50 | 值同表头的代码 |
| RowCode | 标识号 | Int | Y | Y | - | 10 | |
| SubMaterialId | 子件代码 | String | - | Y | - | 40 | 值同erp后台的物料代码 |
| UnitQty | 用量 | Decimal | - | Y | 9 | 19 | |
| BaseQty | 母件基数 | Decimal | - | - | 9 | 19 | |
| SubMatType | 子件来源 | Byte | - | - | - | 3 | 下拉选项：0自备料件 1客户提供 2厂商提供 4文本项目 |

##### 表4: BOMSubMatInstallInfo（BOM子件安装点信息，5个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| BOMKeyId | BOM代码 | String | Y | Y | - | 50 | |
| ParentRowCode | 父标识号 | Int | Y | Y | - | 10 | |
| RowCode | 标识号 | Int | Y | Y | - | 10 | |
| Description | 安装点说明 | String | - | Y | - | 100 | |
| Quantity | 使用数量 | Decimal | - | Y | 9 | 19 | |

##### 表5: plsBOMSubReplacementDetail（BOM替代料明细，6个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| BOMKeyId | BOM代码 | String | Y | Y | - | 50 | |
| RowCode | 标识号 | Int | Y | Y | - | 10 | |
| ParentRowCode | 父标识号 | Int | Y | Y | - | 10 | |
| ReplacementId | 替代物料 | String | - | Y | - | 40 | |
| BaseQty | 原子件基数 | Decimal | - | Y | 9 | 19 | |
| UnitQty | 单位替代数量 | Decimal | - | Y | 9 | 19 | |

#### getdata 成功响应示例

```json
{
    "data": {
        "BOMMainInfo": {
            "BOMKeyId": "",
            "BOMKeyName": "",
            "MaterialId": "",
            "BizAttr": 0,
            "Version": "",
            "BOMStyleId": "",
            "BOMTypeId": "",
            "BOMSerNo": 0,
            "FromBizPartnerId": "",
            "FromBillCategory": 0,
            "FromBillNo": "",
            "FromRowCode": 0
        },
        "BOMSubMatBatchQtyInfo": [],
        "BOMSubMatInfo": [],
        "BOMSubMatInstallInfo": [],
        "plsBOMSubReplacementDetail": []
    },
    "status": 1,
    "trcode": 17765,
    "cltrcode": 0,
    "etime": 20240702132548,
    "eduration": 65603
}
```

---

## 6. purReceivingOrder_RC（采购收货单）- 清单查询 + 明细查询

### 6.1 purReceivingOrder_RC getlist（清单查询）

#### 调用方式

| 项目 | 值 |
|------|------|
| 接口地址 | `/esb/erp/getlist.do` |
| progid | `purReceivingOrder_RC` |

#### 请求参数

（同 getlist 通用请求参数，progid 值为 `purReceivingOrder_RC`）

#### 请求参数示例

```json
{
    "token": "E543F9D3D4FA017BC02F9E4BBCEEE6AB",
    "time": "20211117015449",
    "sign": "A3FF17E66DF57507FF51EBCBE4B0C7E5",
    "progid": "purReceivingOrder_RC",
    "data": {
        "condition": "BillNo = ''",
        "lastpkvalues": ""
    }
}
```

#### 数据字典 - purReceivingOrderMaster 表（6个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| BillNo | 单据编号 | String | Y | - | - | 20 | |
| TypeId | 单据类型 | String | - | - | - | 10 | |
| BillDate | 单据日期 | String | - | - | - | 10 | 格式要求:YYYY-MM-DD 举例2023-09-19 |
| BizPartnerId | 供应商 | String | - | - | - | 20 | 值同ERP后台的供应商代码 |
| PersonId | 采购人员代码 | String | - | - | - | 20 | 值同ERP后台的人员代码 |
| IsPriceWithTax | 单价含税 | Boolean | - | - | - | 1 | 0不含税;1含税 |

#### getlist 成功响应示例

```json
{
    "data": {
        "purReceivingOrderMaster": [
            {
                "BillNo": "",
                "TypeId": "",
                "BillDate": "2025-11-14",
                "BizPartnerId": "",
                "PersonId": "",
                "IsPriceWithTax": false
            }
        ]
    },
    "lastpkvalues": "",
    "hasnext": false,
    "status": 1,
    "trcode": 17765,
    "cltrcode": 0,
    "etime": 20240702132548,
    "eduration": 65603
}
```

### 6.2 purReceivingOrder_RC getdata（明细查询）

#### 调用方式

| 项目 | 值 |
|------|------|
| 接口地址 | `/esb/erp/get.do` |
| progid | `purReceivingOrder_RC` |

#### 请求参数

（同 getdata 通用请求参数，progid 值为 `purReceivingOrder_RC`）

#### 请求参数示例

```json
{
    "token": "E543F9D3D4FA017BC02F9E4BBCEEE6AB",
    "time": "20211117015449",
    "sign": "A3FF17E66DF57507FF51EBCBE4B0C7E5",
    "progid": "purReceivingOrder_RC",
    "data": {
        "pkvalues": ""
    }
}
```

#### 数据字典 - 共2个数据表

##### 表1: purReceivingOrderMaster（采购收货单表头，6个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| BillNo | 单据编号 | String | Y | - | - | 20 | |
| TypeId | 单据类型 | String | - | - | - | 10 | |
| BillDate | 单据日期 | String | - | - | - | 10 | 格式要求:YYYY-MM-DD 举例2023-09-19 |
| BizPartnerId | 供应商 | String | - | - | - | 20 | 值同ERP后台的供应商代码 |
| PersonId | 采购人员代码 | String | - | - | - | 20 | 值同ERP后台的人员代码 |
| IsPriceWithTax | 单价含税 | Boolean | - | - | - | 1 | 0不含税;1含税 |

##### 表2: purReceivingOrderDetail（采购收货单明细，12个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| RowCode | 标识号 | Int | Y | - | - | 10 | |
| ItemType | 类型 | Byte | - | - | - | 3 | |
| MaterialId | 物料代码 | String | - | - | - | 40 | 值同ERP后台物料代码 |
| MaterialSpec | 物料规格 | String | - | - | - | 200 | |
| TaxId | 税代码 | String | - | - | - | 20 | 值同ERP后台税码 |
| SPrice | 交易价 | Decimal | - | - | 9 | 19 | |
| ReceivingSQty | 收货数量 | Decimal | - | - | 9 | 19 | |
| OAmount | 金额 | Decimal | - | - | 6 | 19 | |
| OAmountWithTax | 含税金额 | Decimal | - | - | 6 | 19 | |
| FromSourceTag | 来源单种类 | Int | - | - | - | 10 | 传值说明：2601<采购订单> |
| FromBillNo | 来源单号 | String | - | - | - | 20 | |
| FromRowCode | 来源标识号 | Int | - | - | - | 10 | |

---

## 7. ppProduceOrder（生产订单）- 清单查询

### 调用方式

| 项目 | 值 |
|------|------|
| 接口地址 | `/esb/erp/getlist.do` |
| progid | `ppProduceOrder` |

### 请求参数

（同 getlist 通用请求参数，progid 值为 `ppProduceOrder`）

### 请求参数示例

```json
{
    "token": "E543F9D3D4FA017BC02F9E4BBCEEE6AB",
    "time": "20211117015449",
    "sign": "A3FF17E66DF57507FF51EBCBE4B0C7E5",
    "progid": "ppProduceOrder",
    "data": {
        "condition": "BillNo = ''",
        "lastpkvalues": ""
    }
}
```

### 数据字典 - ppProduceOrder 表（35个字段）

| 字段 | 名称 | 类型 | 主键 | 必填 | 小数位数 | 长度 | 备注 |
|------|------|------|------|------|----------|------|------|
| BillNo | 单据编号 | String | Y | - | - | 20 | |
| BillDate | 单据日期 | String | - | Y | - | 10 | |
| TypeId | 单据类型 | String | - | Y | - | 10 | |
| MaterialId | 母件 | String | - | Y | - | 40 | |
| ProduceQty | 生产数量 | Decimal | - | Y | 9 | 19 | |
| DemandBeginDate | 需求开始日期 | String | - | Y | - | 10 | |
| DemandCompleteDate | 需求完工日期 | String | - | Y | - | 10 | |
| DemandStockInDate | 需求入库日期 | String | - | Y | - | 10 | |
| IsSubcontract | 委外生产 | Boolean | - | - | - | 1 | |
| ProduceBatchNo | 生产批号 | String | - | - | - | 60 | |
| FromBOMKeyId | BOM | String | - | - | - | 50 | |
| FromTechRouteKeyId | 工艺路线 | String | - | - | - | 50 | |
| WorkCenterId | 工作中心代码 | String | - | - | - | 20 | |
| IssueTime | 下达时间 | String | - | - | - | 19 | |
| BizPartnerId | 客户 | String | - | - | - | 20 | |
| ParentSourceTag | 母制令单据种类 | Int | - | - | - | 10 | |
| ParentBillNo | 母制令单据编号 | String | - | - | - | 20 | |
| ParentRowCode | 单据行标识号 | Int | - | - | - | 10 | |
| IsOpeningData | 期初数据 | Boolean | - | - | - | 1 | |
| Remark | 备注 | String | - | - | - | 2000 | |
| CheckType | 检验方法 | Byte | - | - | - | 3 | |
| CertifiedQty | 合格基本数量 | Decimal | - | - | 9 | 19 | |
| DiscertifiedQty | 不合格基本数量 | Decimal | - | - | 9 | 19 | |
| TransCheckQty | 已转检验基本数量 | Decimal | - | - | 9 | 19 | |
| UntransCheckQty | 未转检验基本数量 | Decimal | - | - | 9 | 19 | |
| CheckCount | 检验次数 | Int | - | - | - | 10 | |
| EndDate | 结束日期 | String | - | - | - | 10 | |
| EPrjId | 项目 | String | - | - | - | 20 | |
| StockInQty | 入库数量 | Decimal | - | - | 9 | 19 | |
| MaterialSpec | 物料规格 | String | - | - | - | 200 | |
| ProduceState | 生产状态 | Byte | - | - | - | 3 | 下拉选项：0未下达; 1已下达; 2部分完工; 3已停工; 4全部完工 |
| AllowWasterQty | 允许废品数量 | Decimal | - | - | 9 | 19 | |
| PlanOutPutQty | 计划产出数量 | Decimal | - | - | 9 | 19 | |
| ProdWasterRate | 废品率 | Decimal | - | - | 7 | 9 | |
| DemandBeginTime | 需求开始时间 | String | - | - | - | 8 | |
| DemandCompleteTime | 需求完工时间 | String | - | - | - | 8 | |
| DemandStockInTime | 需求入库时间 | String | - | - | - | 8 | |
| BizAttr | 业务属性 | Int | - | - | - | 10 | 下拉选项：6标准制令单; 7重工制令单; 9拆解制令单 |

### 响应参数

（同 getlist 通用响应参数）

### 成功响应示例

```json
{
    "data": {
        "ppProduceOrder": [
            {
                "BillNo": "",
                "BillDate": "2025-11-14",
                "TypeId": "",
                "MaterialId": "",
                "ProduceQty": 0,
                "DemandBeginDate": "2025-11-14",
                "DemandCompleteDate": "2025-11-14",
                "DemandStockInDate": "2025-11-14",
                "IsSubcontract": false,
                "ProduceBatchNo": "",
                "FromBOMKeyId": "",
                "FromTechRouteKeyId": "",
                "WorkCenterId": "",
                "IssueTime": "2025-11-14 11:35:12",
                "BizPartnerId": "",
                "ParentSourceTag": 0,
                "ParentBillNo": "",
                "ParentRowCode": 0,
                "IsOpeningData": false,
                "Remark": "",
                "CheckType": 0,
                "CertifiedQty": 0,
                "DiscertifiedQty": 0,
                "TransCheckQty": 0,
                "UntransCheckQty": 0,
                "CheckCount": 0,
                "EndDate": "2025-11-14",
                "EPrjId": "",
                "StockInQty": 0,
                "MaterialSpec": "",
                "ProduceState": 0,
                "AllowWasterQty": 0,
                "PlanOutPutQty": 0,
                "ProdWasterRate": 0,
                "DemandBeginTime": "11:35:12",
                "DemandCompleteTime": "11:35:12",
                "DemandStockInTime": "11:35:12",
                "BizAttr": 0
            }
        ]
    },
    "lastpkvalues": "",
    "hasnext": false,
    "status": 1,
    "trcode": 17765,
    "cltrcode": 0,
    "etime": 20240702132548,
    "eduration": 65603
}
```

---

## 字段统计汇总

| 接口 | progid | 数据表 | 字段数量 |
|------|--------|--------|----------|
| 认证接口 | - | 请求参数 | 4 |
| 认证接口 | - | 成功响应 | 9 |
| 认证接口 | - | 失败响应 | 6 |
| comMaterialGroup getlist | comMaterialGroup | MaterialGroup | 9 |
| comMaterial getlist | comMaterial | companyMaterial | 13 |
| ppBOM getlist | ppBOM | BOMMainInfo | 12 |
| ppBOM getdata | ppBOM | BOMMainInfo | 12 |
| ppBOM getdata | ppBOM | BOMSubMatBatchQtyInfo | 6 |
| ppBOM getdata | ppBOM | BOMSubMatInfo | 6 |
| ppBOM getdata | ppBOM | BOMSubMatInstallInfo | 5 |
| ppBOM getdata | ppBOM | plsBOMSubReplacementDetail | 6 |
| purReceivingOrder_RC getlist | purReceivingOrder_RC | purReceivingOrderMaster | 6 |
| purReceivingOrder_RC getdata | purReceivingOrder_RC | purReceivingOrderMaster | 6 |
| purReceivingOrder_RC getdata | purReceivingOrder_RC | purReceivingOrderDetail | 12 |
| ppProduceOrder getlist | ppProduceOrder | ppProduceOrder | 35 |
