/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2023 abel533@gmail.com
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
 */

package cn.cordys.common.handler;

import com.github.pagehelper.parser.CountJSqlParser51;
import com.github.pagehelper.parser.SqlParserUtil;
import net.sf.jsqlparser.parser.Token;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

// shortcut: sqlparser5.1 6.1.1 的 AST 返回类型与 JSqlParser 5.4 不兼容，官方发布修复后删除此覆盖。
// 保留官方 Count 算法，仅将 getASTNode() 调用重新编译到 5.4 的 Node 类型。
public class PageHelperCountSqlParser extends CountJSqlParser51 {
    @Override
    public String getSmartCountSql(String sql, String countColumn) {
        Statement stmt;
        if (sql.contains(KEEP_ORDERBY) || keepOrderBy()) {
            return getSimpleCountSql(sql, countColumn);
        }
        try {
            stmt = SqlParserUtil.parse(sql);
        } catch (Throwable e) {
            return getSimpleCountSql(sql, countColumn);
        }
        Select select = (Select) stmt;
        try {
            processSelect(select);
        } catch (Exception e) {
            return getSimpleCountSql(sql, countColumn);
        }
        processWithItemsList(select.getWithItemsList());
        Select countSelect = sqlToCount(select, countColumn);
        String result = countSelect.toString();
        if (select instanceof PlainSelect) {
            Token token = select.getASTNode().jjtGetFirstToken().specialToken;
            if (token != null) {
                String hints = token.toString().trim();
                if (hints.startsWith("/*") && hints.endsWith("*/") && !result.startsWith("/*")) {
                    result = hints + result;
                }
            }
        }
        return result;
    }

}
