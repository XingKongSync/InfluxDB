package influxdb;

import java.util.Stack;

public class JsonDeserializer {
    /**
     * 遇到了无法解析的字符
     */
    public static final int CONST_RETURN_ERROR_CHAR_INVALID_OCCURED = -1;

    /**
     * Json语法不正确
     */
    public static final int CONST_RETURN_ERROR_SYNTAX_OCCURED = -2;

    /**
     * 遇到了无法转义的字符
     */
    public static final int CONST_RETURN_ERROR_INVALID_ESCAPE_CHAR = -3;

    /**
     * 解析数值出错
     */
    public static final int CONST_RETURN_ERROR_PARSE_NUMBER = -4;

    /**
     * 解析null出错
     */
    public static final int CONST_RETURN_ERROR_PARSE_NULL = -5;

    /**
     * 解析器内部错误
     */
    public static final int CONST_RETURN_ERROR_INNER_EXCEPTION = -6;

    private static final int CONST_CHAR_TYPE_INVALID = -1;
    private static final int CONST_CHAR_TYPE_KEY_SYMBOl = 0;
    private static final int CONST_CHAR_TYPE_NUM = 1;
    private static final int CONST_CHAR_TYPE_LETTER = 2;
    private static final int CONST_CHAR_TYPE_BLANK_SYMBOL = 3;

    /**
     * 遇到了无法转义的字符
     */
    private static final int CONST_ERROR_INVALID_ESCAPE_CHAR = 0;

    private static final char[] KeySymbol = new char[] { '{', '}', '[', ']', ':', ',', '\\', '\"' };
    private static final char[] BlankSymbol = new char[] { ' ', '\t', '\r', '\n' };

    private String lastError;
    private StringBuilder stringBuffer;
    private boolean isProcessingNumber = false;//stringBuffer中是否是数字或者null字符的标志
    private boolean isScientific = false;//stringBuffer中的数值是否以科学计数法表示
    private boolean isFloat = false;//stringBuffer中的数字是否是float类型的标志
    private boolean isWaitingNull = false;//是否等待接收null的字符
    private boolean isStringReady = false;//是否已经接收了一个完整的字符串

    private JsonObject rootJsonObject;//Json祖先对象

    /**
     * Json元素栈
     * 当出栈元素是JsonToken的时候，此元素会加入到栈顶的JsonObject中
     * 当出栈元素是JsonObject的时候，此元素会赋值为栈顶的JsonToken的Value
     * 当出栈元素是JsonObject并且栈空，则说明Json解析完毕
     */
    private Stack<IJsonElement> pairStack;

    /**
     * 符号栈，用于匹配符号
     */
    private Stack<Character> symbolStack;

    public JsonDeserializer() {
        stringBuffer = new StringBuilder();
        pairStack = new Stack<IJsonElement>();
        symbolStack = new Stack<Character>();
    }

    public String getLastError() {
        return lastError;
    }

    private void setLastError(String lastError, boolean doPrint) {
        this.lastError = lastError;
        if (doPrint) {
            System.err.println(lastError);
        }
    }

    /**
     * 解析Json片段
     * @param inputBuffer Json片段缓冲区
     * @param inputLength 缓冲区长度
     * @return
     */
    public int InputData(char[] inputBuffer, int inputLength) {
        for (int i = 0; i < inputLength; i++) {
            // System.out.print(inputBuffer[i]);
            int result = ScanChar(inputBuffer[i]);
            // System.out.println("scan char: " + inputBuffer[i] + " result: " + result);
            if (result != 0) {
                String errDesc = "在\r\n" + String.valueOf(inputBuffer, 0 , inputLength) + "\r\n附近发生解析错误，索引：" + i;
                if (result == CONST_RETURN_ERROR_CHAR_INVALID_OCCURED) {
                    setLastError(errDesc + "原因：遇到非法字符", false);
                } else if (result == CONST_RETURN_ERROR_SYNTAX_OCCURED) {
                    setLastError(errDesc + "原因：Json语法错误", false);
                } else if (result == CONST_RETURN_ERROR_INVALID_ESCAPE_CHAR) {
                    setLastError(errDesc + "原因：遇到了无法转义的字符", false);
                } else if (result == CONST_RETURN_ERROR_PARSE_NUMBER) {
                    setLastError(errDesc + "原因：解析数字失败", false);
                } else if (result == CONST_RETURN_ERROR_PARSE_NULL) {
                    setLastError(errDesc + "原因：解析null失败", false);
                }
                return result;
            }
        }
        return 0;
    }

    private int ScanChar(char c) {
        //先处理栈顶是引号（"）或者反斜杠（\）的情况
        if (!symbolStack.isEmpty()) {
            char symbolTopChar = symbolStack.peek();
            if (symbolTopChar == '\"' && c != '\"' && c != '\\') {
                //如果栈顶是引号，并且c不是引号，也不是反斜杠，则其后的字符均视为字符串的一部分
                stringBuffer.append(c);
                return 0;
            }
            if (symbolTopChar == '\"' && c == '\\') {
                //如果栈顶是引号，并且c是反斜杠，则说明接下来的字符需要转义，反斜杠入栈
                symbolStack.push(c);
            }
            if (symbolTopChar == '\"' && c == '\"') {
                //如果栈顶是引号，并且c是引号，出栈
                //标记为已经接收了一个完整的字符串
                isStringReady = true;
                symbolStack.pop();
                return 0;
            }
            if (symbolTopChar == '\\') {
                //如果栈顶符号是反斜杠，则对本次扫描的字符进行转义
                symbolStack.pop();
                char result = EscapChar(c);
                if (result == CONST_ERROR_INVALID_ESCAPE_CHAR) {
                    //字符无法转义
                    return CONST_RETURN_ERROR_INVALID_ESCAPE_CHAR;
                } else {
                    //将转义后的字符串添加到字符串缓冲区
                    stringBuffer.append(c);
                }
                return 0;
            }
        }

        //当正在接收数字的时候，遇到e、+、-时，视为数字的一部分
        if (isProcessingNumber && (c == 'e' || c == '+' || c == '-')) {
            isScientific = true;
            isFloat = true;
            stringBuffer.append(c);
            return 0;
        }

        //处理其他情况
        int charType = GetCharType(c);
        switch (charType) {
        case CONST_CHAR_TYPE_INVALID:
            //遇到了无法解析的字符
            return CONST_RETURN_ERROR_CHAR_INVALID_OCCURED;
        case CONST_CHAR_TYPE_KEY_SYMBOl:
            //关键符号
            return ProcessKeySymbolChar(c);
        case CONST_CHAR_TYPE_NUM:
            //数字或者小数点
            isProcessingNumber = true;//标记为数字类型
            if (c == '.') {
                isFloat = true;//标记为float型
            }
            //先把数字都放入字符串缓冲区中，等遇到逗号的时候再判断数值是否合法
            stringBuffer.append(c);
            break;
        case CONST_CHAR_TYPE_LETTER:
            //遇到字母
            //当符号栈顶不是引号（"）的情况下遇到字母，只可能是null
            //先把字母都放入字符串缓冲区中，等遇到逗号的时候再判断null是否正确
            isWaitingNull = true;//标记正在接收null的字符
            stringBuffer.append(c);
            break;
        case CONST_CHAR_TYPE_BLANK_SYMBOL:
            //遇到空白字符则忽略
            break;
        default:
            //解析器内部错误
            return CONST_RETURN_ERROR_INNER_EXCEPTION;
        }
        return 0;
    }

    private int ProcessKeySymbolChar(char c) {
        if (c == '{') {
            //左括号入栈
            symbolStack.push(c);
            //创建JsonObject
            JsonObject jsonObject = new JsonObject();
            jsonObject.setObjType(JsonObject.CONST_JSON_OBJECT_TYPE_OBJECT);
            //检查栈顶
            if (!pairStack.isEmpty()) {
                IJsonElement topElement = pairStack.peek();
                if (topElement.getJsonElementType() == JsonElementType.CONST_JSON_OBJECT) {
                    JsonObject topJsonObject = topElement.getJsonObject();
                    //如果栈顶是JsonObject的话，则将此JsonObject添加为其的JsonToken，并且该JsonToken没有Key
                    JsonToken token = new JsonToken();
                    token.setHasKey(false);
                    token.setValueType(JsonToken.CONST_JSON_FIELD_TYPE_OBJECT);
                    token.setoValue(jsonObject);
                    topJsonObject.getTokenList().add(token);
                } else {
                    //如果栈顶是JsonToken的话，则将此JsonObject添加为其的Value
                    JsonToken token = topElement.getJsonToken();
                    token.setoValue(jsonObject);
                    token.setValueType(JsonToken.CONST_JSON_FIELD_TYPE_OBJECT);
                    //token出栈
                    pairStack.pop();
                }
            }
            //放入Json元素栈
            pairStack.push(jsonObject);
        } else if (c == '[') {
            //左括号入栈
            symbolStack.push(c);
            //创建JsonObject Array
            JsonObject jsonArray = new JsonObject();
            jsonArray.setObjType(JsonObject.CONST_JSON_OBJECT_TYPE_ARRAY);
            //检查栈顶
            if (!pairStack.isEmpty()) {
                IJsonElement topElement = pairStack.peek();
                if (topElement.getJsonElementType() == JsonElementType.CONST_JSON_OBJECT) {
                    JsonObject topJsonObject = topElement.getJsonObject();
                    //如果栈顶是JsonObject的话，则为其添加一个没有key的JsonObject（JsonArray）
                    JsonToken token = new JsonToken();
                    token.setHasKey(false);
                    token.setValueType(JsonToken.CONST_JSON_FIELD_TYPE_OBJECT);
                    token.setoValue(jsonArray);
                    topJsonObject.getTokenList().add(token);
                } else {
                    //如果栈顶是JsonToken的话，则将此JsonObject（JsonArray）添加为其的Value
                    JsonToken token = topElement.getJsonToken();
                    token.setValueType(JsonToken.CONST_JSON_FIELD_TYPE_OBJECT);
                    token.setoValue(jsonArray);
                    //token出栈
                    pairStack.pop();
                }
            }
            //放入Json元素栈
            pairStack.push(jsonArray);
        } else if (c == '\"') {
            //如果是引号，则先入栈
            symbolStack.push(c);
        } else if (c == '}') {
            //JsonObject出栈
            if (symbolStack.peek() != '{') {
                //符号栈栈顶不是'{'，说明json语法错误
                return CONST_RETURN_ERROR_SYNTAX_OCCURED;
            }
            symbolStack.pop();
            if (!pairStack.isEmpty()) {
                IJsonElement top = pairStack.peek();
                if (top.getJsonElementType() == JsonElementType.CONST_JSON_OBJECT
                        && top.getJsonObject().getJsonElementType() == JsonObject.CONST_JSON_OBJECT_TYPE_OBJECT) {
                    JsonObject jsonObject = pairStack.pop().getJsonObject();
                    if (pairStack.isEmpty()) {
                        rootJsonObject = jsonObject;
                    }
                } else if (top.getJsonElementType() == JsonElementType.CONST_JSON_TOKEN) {
                    //为token赋值
                    JsonToken token = top.getJsonToken();
                    int result = SetJsonValueFromStringBuffer(token);
                    token.setHasKey(true);
                    //token出栈
                    pairStack.pop();
                    if (!pairStack.isEmpty()
                            && pairStack.peek().getJsonElementType() == JsonElementType.CONST_JSON_OBJECT) {
                        JsonObject topObj = pairStack.pop().getJsonObject();
                        if (pairStack.isEmpty()) {
                            rootJsonObject = topObj;
                        }
                    } else {
                        return CONST_RETURN_ERROR_SYNTAX_OCCURED;
                    }
                    return result;
                } else {
                    //栈顶不是JsonObject，也不是JsonToken，说明Json语法错误
                    return CONST_RETURN_ERROR_SYNTAX_OCCURED;
                }
            } else {
                //栈为空，说明Json语法错误
                return CONST_RETURN_ERROR_SYNTAX_OCCURED;
            }
        } else if (c == ']') {
            if (symbolStack.peek() != '[') {
                //符号栈栈顶不是'['，说明json语法错误
                return CONST_RETURN_ERROR_SYNTAX_OCCURED;
            }
            symbolStack.pop();
            //JsonObject（JsonArray）出栈
            if (!pairStack.isEmpty()) {
                IJsonElement top = pairStack.peek();
                if (top.getJsonElementType() == JsonElementType.CONST_JSON_OBJECT
                        && top.getJsonObject().getObjType() == JsonObject.CONST_JSON_OBJECT_TYPE_ARRAY) {
                    //检查是否有未处理的字符串、数字、null
                    if (IsStringBufferReady()) {
                        JsonToken token = new JsonToken();
                        int result = SetJsonValueFromStringBuffer(token);
                        if (result != 0) {
                            //发生错误
                            return result;
                        }
                        token.setHasKey(false);
                        top.getJsonObject().getTokenList().add(token);
                    }
                    pairStack.pop();
                } else {
                    //栈顶不是JsonObject（JsonArray），说明Json语法错误
                    return CONST_RETURN_ERROR_SYNTAX_OCCURED;
                }
            } else {
                //栈为空，说明Json语法错误
                return CONST_RETURN_ERROR_SYNTAX_OCCURED;
            }
        } else if (c == ':') {
            //当遇到冒号（:）时，栈顶必须是一个JsonObject
            if (!pairStack.isEmpty()) {
                IJsonElement top = pairStack.peek();
                if (top.getJsonElementType() == JsonElementType.CONST_JSON_OBJECT
                        && top.getJsonObject().getObjType() == JsonObject.CONST_JSON_OBJECT_TYPE_OBJECT) {
                    //栈顶是JsonObject，向该JsonObject添加JsonToken
                    JsonObject topJsonObject = top.getJsonObject();
                    JsonToken token = new JsonToken();
                    //为JsonToken赋值
                    token.setHasKey(true);
                    token.setKey(stringBuffer.toString());
                    CleanStringBuffer();//清空字符串缓冲区
                    topJsonObject.getTokenList().add(token);
                    //token入栈
                    pairStack.push(token);
                } else {
                    //栈顶不是JsonObject，说明Json语法错误
                    return CONST_RETURN_ERROR_SYNTAX_OCCURED;
                }
            } else {
                //栈为空，说明Json语法错误
                return CONST_RETURN_ERROR_SYNTAX_OCCURED;
            }
        } else if (c == ',') {
            //栈空状态肯定是Json不合法
            if (pairStack.isEmpty()) {
                return CONST_RETURN_ERROR_SYNTAX_OCCURED;
            }
            IJsonElement topJsonElement = pairStack.peek();
            //当栈顶是个JsonArray并且字符串缓冲区准备完成后的情况，向JsonArray添加无Key的JsonToken
            if (topJsonElement.getJsonElementType() == JsonElementType.CONST_JSON_OBJECT
                    && topJsonElement.getJsonObject().getObjType() == JsonObject.CONST_JSON_OBJECT_TYPE_ARRAY
                    && IsStringBufferReady()) {
                return SetJsonValueFromStringBuffer(topJsonElement.getJsonObject());
            }
            //当栈顶是个JsonToken的情况，则为该JsonToken赋值
            if (topJsonElement.getJsonElementType() == JsonElementType.CONST_JSON_TOKEN) {
                JsonToken token = topJsonElement.getJsonToken();
                int result = SetJsonValueFromStringBuffer(token);
                //JsonToken出栈
                pairStack.pop();
                return result;
            }
            //当栈顶是个JsonArray，但是字符串缓冲区没有数据时，忽略逗号
            return 0;
        }
        return 0;
    }

    /**
     * stringBuffer是否已经缓冲了完整的数据
     * @return 
     */
    private boolean IsStringBufferReady() {
        if (isProcessingNumber || isFloat || isWaitingNull || isStringReady) {
            return true;
        } else {
            return false;
        }
    }

    private int SetJsonValueFromStringBuffer(JsonObject jsonObject) {
        JsonToken token = new JsonToken();
        int result = SetJsonValueFromStringBuffer(token);
        token.setHasKey(false);
        if (result == 0) {
            jsonObject.getTokenList().add(token);
        }
        return result;
    }

    private int SetJsonValueFromStringBuffer(JsonToken token) {
        int result = CONST_RETURN_ERROR_SYNTAX_OCCURED;//其他情况均为Json语法错误
        if (isProcessingNumber) {
            //如果当前处于接收数字字符的状态
            if (isFloat) {
                //如果当前数字是浮点型
                token.setValueType(JsonToken.CONST_JSON_FIELD_TYPE_NUMBER);
                token.setNumberType(JsonToken.CONST_JSON_NUMBER_TYPE_FLOAT);
                try {
                    float fValue = Float.parseFloat(stringBuffer.toString());
                    token.setfValue(fValue);
                } catch (Exception e) {
                    //解析浮点型数值失败
                    setLastError(e.getMessage(), false);
                    result = CONST_RETURN_ERROR_PARSE_NUMBER;
                }
            } else {
                //如果当前数字是整型
                token.setValueType(JsonToken.CONST_JSON_FIELD_TYPE_NUMBER);
                token.setNumberType(JsonToken.CONST_JSON_NUMBER_TYPE_INT);
                try {
                    int iValue = Integer.parseInt(stringBuffer.toString());
                    token.setiValue(iValue);
                } catch (Exception e) {
                    //解析整型数值失败
                    setLastError(e.getMessage(), false);
                    result = CONST_RETURN_ERROR_PARSE_NUMBER;
                }
            }
            //重置数字接收标志
            isProcessingNumber = false;
            isFloat = false;
            isScientific = false;
            result = 0;
        } else if (isWaitingNull) {
            //如果当前处于接收数字null的状态
            if (stringBuffer.length() == 4 && stringBuffer.toString().equals("null")) {
                token.setValueType(JsonToken.CONST_JSON_FIELD_TYPE_OBJECT);
                token.setoValue(null);
                isWaitingNull = false;
                result = 0;
            } else {
                result = CONST_RETURN_ERROR_PARSE_NULL;
            }
        } else if (isStringReady) {
            //如果当前处于已经接收了一个完整的字符串的状态
            token.setValueType(JsonToken.CONST_JSON_FIELD_TYPE_STRING);
            token.setsValue(stringBuffer.toString());
            result = 0;
        }
        CleanStringBuffer();//清空字符串缓冲区
        return result;
    }

    private void CleanStringBuffer() {
        stringBuffer.setLength(0);
        isProcessingNumber = false;
        isFloat = false;
        isWaitingNull = false;
        isStringReady = false;
    }

    private char EscapChar(char c) {
        switch (c) {
        case '\\':
            return '\\';
        case '\"':
            return '\"';
        case 't':
            return '\t';
        case 'r':
            return '\r';
        case 'n':
            return '\n';
        default:
            return CONST_ERROR_INVALID_ESCAPE_CHAR;
        }
    }

    private int GetCharType(char c) {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            //说明字符是字母
            return CONST_CHAR_TYPE_LETTER;
        } else if ((c >= '0' && c <= '9') || c == '.') {
            //说明字符是数字或者小数点
            return CONST_CHAR_TYPE_NUM;
        } else {
            //检查是否是关键符号
            for (int i = 0; i < KeySymbol.length; i++) {
                if (KeySymbol[i] == c) {
                    //说明是关键符号
                    return CONST_CHAR_TYPE_KEY_SYMBOl;
                }
            }
            //检查是否是空白字符
            for (int i = 0; i < BlankSymbol.length; i++) {
                if (BlankSymbol[i] == c) {
                    //说明是空白符号
                    return CONST_CHAR_TYPE_BLANK_SYMBOL;
                }
            }
            return CONST_CHAR_TYPE_INVALID;
        }
    }

    public JsonObject EndInput() {
        if (!pairStack.isEmpty()) {
            IJsonElement topJsonElement = pairStack.pop();
            if (topJsonElement.getJsonElementType() == JsonElementType.CONST_JSON_OBJECT) {
                return topJsonElement.getJsonObject();
            }
        }
        return rootJsonObject;
    }
}
