import { z } from 'zod'
import type { CustomFieldDefinition } from '../types'

export const assetCoreSchema = z
  .object({
    categoryId: z.string().min(1, 'Category is required'),
    name: z.string().min(1, 'Name is required').max(160),
    manufacturer: z.string().max(150).optional().or(z.literal('')),
    model: z.string().max(150).optional().or(z.literal('')),
    serialNumber: z.string().max(150).optional().or(z.literal('')),
    vendorName: z.string().max(200).optional().or(z.literal('')),
    purchaseOrderReference: z.string().max(100).optional().or(z.literal('')),
    purchaseDate: z.string().optional().or(z.literal('')),
    purchaseCost: z
      .union([z.coerce.number().min(0, 'Must not be negative'), z.literal('')])
      .optional(),
    warrantyStartDate: z.string().optional().or(z.literal('')),
    warrantyEndDate: z.string().optional().or(z.literal('')),
  })
  .refine(
    (v) => !v.warrantyStartDate || !v.warrantyEndDate || v.warrantyEndDate >= v.warrantyStartDate,
    { message: 'Must not be before warranty start date', path: ['warrantyEndDate'] },
  )

export type AssetCoreFormValues = z.infer<typeof assetCoreSchema>

/**
 * Builds a Zod schema at runtime from a category's custom-field definitions -
 * mirrors the backend's CustomFieldValidationService so client-side feedback
 * matches server behavior, while the server (not this) remains authoritative.
 */
export function buildCustomFieldsSchema(definitions: CustomFieldDefinition[]) {
  const shape: Record<string, z.ZodTypeAny> = {}

  for (const def of definitions) {
    let fieldSchema: z.ZodTypeAny
    switch (def.dataType) {
      case 'TEXT':
        fieldSchema = z.string()
        break
      case 'NUMBER':
        fieldSchema = z.coerce.number({ error: 'Must be a number' })
        break
      case 'DATE':
        fieldSchema = z.string().refine((v) => !Number.isNaN(Date.parse(v)), 'Must be a valid date')
        break
      case 'BOOLEAN':
        fieldSchema = z.boolean()
        break
      case 'ENUM':
        fieldSchema = z.enum((def.enumOptions ?? ['']) as [string, ...string[]])
        break
      default:
        fieldSchema = z.any()
    }

    if (!def.required) {
      fieldSchema = fieldSchema.optional().or(z.literal(''))
    } else if (def.dataType === 'TEXT') {
      fieldSchema = z.string().min(1, 'This field is required')
    }

    shape[def.fieldKey] = fieldSchema
  }

  return z.object(shape)
}
