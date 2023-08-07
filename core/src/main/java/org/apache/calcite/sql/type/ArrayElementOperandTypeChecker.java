/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.sql.type;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlCallBinding;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperandCountRange;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlUtil;

import com.google.common.collect.ImmutableList;

import static org.apache.calcite.sql.type.NonNullableAccessors.getComponentTypeOrThrow;
import static org.apache.calcite.util.Static.RESOURCE;

/**
 * Parameter type-checking strategy where types must be Array and Array element type.
 */
public class ArrayElementOperandTypeChecker implements SqlOperandTypeChecker {
  //~ Instance fields --------------------------------------------------------

  private final boolean allowNullCheck;
  private final boolean allowCast;

  //~ Constructors -----------------------------------------------------------

  public ArrayElementOperandTypeChecker() {
    this.allowNullCheck = false;
    this.allowCast = false;
  }

  public ArrayElementOperandTypeChecker(boolean allowNullCheck, boolean allowCast) {
    this.allowNullCheck = allowNullCheck;
    this.allowCast = allowCast;
  }

  //~ Methods ----------------------------------------------------------------

  @Override public boolean checkOperandTypes(
      SqlCallBinding callBinding,
      boolean throwOnFailure) {
    if (allowNullCheck) {
      // no operand can be null for type-checking to succeed
      for (SqlNode node : callBinding.operands()) {
        if (SqlUtil.isNullLiteral(node, allowCast)) {
          if (throwOnFailure) {
            throw callBinding.getValidator().newValidationError(node, RESOURCE.nullIllegal());
          } else {
            return false;
          }
        }
      }
    }

    final SqlNode op0 = callBinding.operand(0);
    if (!OperandTypes.ARRAY.checkSingleOperandType(
        callBinding,
        op0,
        0,
        throwOnFailure)) {
      return false;
    }

    RelDataType arrayComponentType =
        getComponentTypeOrThrow(SqlTypeUtil.deriveType(callBinding, op0));
    final SqlNode op1 = callBinding.operand(1);
    RelDataType aryType1 = SqlTypeUtil.deriveType(callBinding, op1);

    RelDataType biggest =
        callBinding.getTypeFactory().leastRestrictive(
            ImmutableList.of(arrayComponentType, aryType1));
    if (biggest == null) {
      if (throwOnFailure) {
        throw callBinding.newError(
            RESOURCE.typeNotComparable(
                arrayComponentType.toString(), aryType1.toString()));
      }

      return false;
    }
    return true;
  }

  @Override public SqlOperandCountRange getOperandCountRange() {
    return SqlOperandCountRanges.of(2);
  }

  @Override public String getAllowedSignatures(SqlOperator op, String opName) {
    return "<ARRAY> " + opName + " <ARRAY>";
  }
}
