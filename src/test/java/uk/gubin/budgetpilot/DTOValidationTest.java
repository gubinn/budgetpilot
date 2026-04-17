package uk.gubin.budgetpilot;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gubin.budgetpilot.dto.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * DTO 校验测试 —— 验证所有 @NotNull、@Positive、@Min、@Max、@NotBlank 约束。
 */
class DTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ============ TransactionCreateDTO ============

    @Nested
    @DisplayName("TransactionCreateDTO 校验")
    class TransactionCreateDTOValidationTests {

        @Test
        @DisplayName("合法数据通过校验")
        void validData_Passes() {
            TransactionCreateDTO dto = buildValidTransactionDTO();
            Set<ConstraintViolation<TransactionCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("type 为空 → 失败")
        void nullType_Fails() {
            TransactionCreateDTO dto = buildValidTransactionDTO();
            dto.setType(null);
            Set<ConstraintViolation<TransactionCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("type=999 超出范围 → 失败")
        void invalidType_Fails() {
            TransactionCreateDTO dto = buildValidTransactionDTO();
            dto.setType(999);
            Set<ConstraintViolation<TransactionCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("type=0 超出范围 → 失败")
        void typeZero_Fails() {
            TransactionCreateDTO dto = buildValidTransactionDTO();
            dto.setType(0);
            Set<ConstraintViolation<TransactionCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("type=1,2,3 都通过")
        void validTypes_Pass() {
            for (int t : List.of(1, 2, 3)) {
                TransactionCreateDTO dto = buildValidTransactionDTO();
                dto.setType(t);
                Set<ConstraintViolation<TransactionCreateDTO>> violations = validator.validate(dto);
                assertThat(violations).as("type=%d should pass", t).isEmpty();
            }
        }

        @Test
        @DisplayName("amount 为空 → 失败")
        void nullAmount_Fails() {
            TransactionCreateDTO dto = buildValidTransactionDTO();
            dto.setAmount(null);
            Set<ConstraintViolation<TransactionCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("amount <= 0 → 失败")
        void negativeAmount_Fails() {
            TransactionCreateDTO dto = buildValidTransactionDTO();
            dto.setAmount(new BigDecimal("-50"));
            Set<ConstraintViolation<TransactionCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("accountId 为空 → 失败")
        void nullAccountId_Fails() {
            TransactionCreateDTO dto = buildValidTransactionDTO();
            dto.setAccountId(null);
            Set<ConstraintViolation<TransactionCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("categoryId 为空 → 失败")
        void nullCategoryId_Fails() {
            TransactionCreateDTO dto = buildValidTransactionDTO();
            dto.setCategoryId(null);
            Set<ConstraintViolation<TransactionCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }
    }

    // ============ TransactionUpdateDTO ============

    @Nested
    @DisplayName("TransactionUpdateDTO 校验")
    class TransactionUpdateDTOValidationTests {

        @Test
        @DisplayName("type=999 超出范围 → 失败")
        void invalidType_Fails() {
            TransactionUpdateDTO dto = new TransactionUpdateDTO();
            dto.setType(999);
            Set<ConstraintViolation<TransactionUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("type=1 通过")
        void validType_Pass() {
            TransactionUpdateDTO dto = new TransactionUpdateDTO();
            dto.setType(1);
            Set<ConstraintViolation<TransactionUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("amount <= 0 → 失败")
        void negativeAmount_Fails() {
            TransactionUpdateDTO dto = new TransactionUpdateDTO();
            dto.setAmount(new BigDecimal("-10"));
            Set<ConstraintViolation<TransactionUpdateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }
    }

    // ============ CategoryCreateDTO ============

    @Nested
    @DisplayName("CategoryCreateDTO 校验")
    class CategoryCreateDTOValidationTests {

        @Test
        @DisplayName("合法数据通过校验")
        void validData_Passes() {
            CategoryCreateDTO dto = buildValidCategoryDTO();
            Set<ConstraintViolation<CategoryCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("name 为空 → 失败")
        void blankName_Fails() {
            CategoryCreateDTO dto = buildValidCategoryDTO();
            dto.setName("");
            Set<ConstraintViolation<CategoryCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("type 为空 → 失败")
        void nullType_Fails() {
            CategoryCreateDTO dto = buildValidCategoryDTO();
            dto.setType(null);
            Set<ConstraintViolation<CategoryCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("type=999 超出范围 → 失败")
        void invalidType_Fails() {
            CategoryCreateDTO dto = buildValidCategoryDTO();
            dto.setType(999);
            Set<ConstraintViolation<CategoryCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("type=1,2,3 都通过")
        void validTypes_Pass() {
            for (int t : List.of(1, 2, 3)) {
                CategoryCreateDTO dto = buildValidCategoryDTO();
                dto.setType(t);
                Set<ConstraintViolation<CategoryCreateDTO>> violations = validator.validate(dto);
                assertThat(violations).as("type=%d should pass", t).isEmpty();
            }
        }
    }

    // ============ BudgetCreateDTO ============

    @Nested
    @DisplayName("BudgetCreateDTO 校验")
    class BudgetCreateDTOValidationTests {

        @Test
        @DisplayName("合法数据通过校验")
        void validData_Passes() {
            BudgetCreateDTO dto = buildValidBudgetDTO();
            Set<ConstraintViolation<BudgetCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("yearMonth 为空 → 失败")
        void blankYearMonth_Fails() {
            BudgetCreateDTO dto = buildValidBudgetDTO();
            dto.setYearMonth("");
            Set<ConstraintViolation<BudgetCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("totalAmount 为空 → 失败")
        void nullTotalAmount_Fails() {
            BudgetCreateDTO dto = buildValidBudgetDTO();
            dto.setTotalAmount(null);
            Set<ConstraintViolation<BudgetCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("totalAmount <= 0 → 失败")
        void negativeTotalAmount_Fails() {
            BudgetCreateDTO dto = buildValidBudgetDTO();
            dto.setTotalAmount(new BigDecimal("-100"));
            Set<ConstraintViolation<BudgetCreateDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }
    }

    // ============ HELPERS ============

    private TransactionCreateDTO buildValidTransactionDTO() {
        TransactionCreateDTO dto = new TransactionCreateDTO();
        dto.setType(1);
        dto.setAmount(new BigDecimal("50"));
        dto.setCurrency("CNY");
        dto.setAccountId(1L);
        dto.setCategoryId(10L);
        dto.setTransactionDate(LocalDate.now());
        return dto;
    }

    private CategoryCreateDTO buildValidCategoryDTO() {
        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName("餐饮");
        dto.setType(1);
        return dto;
    }

    private BudgetCreateDTO buildValidBudgetDTO() {
        BudgetCreateDTO dto = new BudgetCreateDTO();
        dto.setYearMonth("2026-04");
        dto.setTotalAmount(new BigDecimal("5000"));
        return dto;
    }
}
