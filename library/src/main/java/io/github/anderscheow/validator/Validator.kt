package io.github.anderscheow.validator

import android.content.Context
import io.github.anderscheow.validator.constant.Mode
import io.github.anderscheow.validator.util.ErrorMessage
import java.util.*

class Validator private constructor(private val context: Context) {

    private var mode = Mode.CONTINUOUS

    interface OnValidateListener {
        @Throws(IndexOutOfBoundsException::class)
        fun onValidateSuccess(values: List<String>)

        fun onValidateFailed()
    }

    fun setMode(mode: Mode): Validator {
        this.mode = mode
        return this
    }

    fun validate(listener: OnValidateListener, vararg validations: Validation) {
        var isOverallValid = true
        var isValid = false
        val values = ArrayList<String>()

        clearAllErrors(*validations)

        // Iterate each validation
        for (validation in validations) {
            val editText = validation.textInputLayout?.editText

            val value = (editText?.text ?: validation.textInput).toString()
            val isCurrentValueValid = validate(value, validation)

            if (isCurrentValueValid) {
                isValid = true
                values.add(value)
            } else {
                isOverallValid = false
                isValid = false

                if (mode == Mode.SINGLE) {
                    return
                }
            }
        }

        if (isValid && isOverallValid) {
            listener.onValidateSuccess(values)
        } else {
            listener.onValidateFailed()
        }
    }

    private fun clearAllErrors(vararg validations: Validation) {
        for (validation in validations) {
            validation.textInputLayout?.error = null
            validation.textInputLayout?.isErrorEnabled = false
        }
    }

    // Iterate each type of rules
    private fun validate(value: String, validation: Validation): Boolean {
        var isCurrentValueValid = validateBaseRules(value, validation)
        if (isCurrentValueValid) {
            isCurrentValueValid = validateAndRules(value, validation)
        }
        if (isCurrentValueValid) {
            isCurrentValueValid = validateOrRules(value, validation)
        }
        if (isCurrentValueValid) {
            isCurrentValueValid = validateConditions(value, validation)
        }

        return isCurrentValueValid
    }

    private fun validateBaseRules(value: String, validation: Validation): Boolean {
        for (baseRule in validation.baseRules) {
            if (!baseRule.validate(value)) {
                showErrorMessage(validation, baseRule)

                return false
            }
        }

        return true
    }

    private fun validateAndRules(value: String, validation: Validation): Boolean {
        for (baseRule in validation.andRules) {
            if (!baseRule.validate(value)) {
                showErrorMessage(validation, baseRule)

                return false
            }
        }

        return true
    }

    private fun validateOrRules(value: String, validation: Validation): Boolean {
        if (validation.orRules.size > 0) {
            val baseRule = validation.orRules[0]

            return if (baseRule.validate(value)) {
                true
            } else {
                showErrorMessage(validation, baseRule)
                false
            }
        }

        return true
    }

    private fun validateConditions(value: String, validation: Validation): Boolean {
        for (condition in validation.conditions) {
            if (!condition.validate(value)) {
                showErrorMessage(validation, condition)

                return false
            }
        }

        return true
    }

    private fun showErrorMessage(validation: Validation, errorMessage: ErrorMessage) {
        if (errorMessage.isErrorAvailable) {
            validation.textInputLayout?.isErrorEnabled = true

            if (errorMessage.isErrorResAvailable) {
                validation.textInputLayout?.error = context.getString(errorMessage.getErrorRes())
            } else if (errorMessage.isErrorMessageAvailable) {
                validation.textInputLayout?.error = errorMessage.getErrorMessage()
            }
        } else {
            throw IllegalStateException("Please either use errorRes or errorMessage as your error output")
        }
    }

    companion object {

        @Deprecated("Use {@link #with(Context)} )} instead.")
        fun getInstance(context: Context): Validator {
            return Validator(context)
        }

        fun with(context: Context): Validator {
            return Validator(context)
        }
    }
}