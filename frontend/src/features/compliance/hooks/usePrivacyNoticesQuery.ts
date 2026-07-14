import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { deletePrivacyNotice, fetchPrivacyNotices, savePrivacyNotice } from '../../../api/compliance/complianceApi'

export function usePrivacyNoticesQuery() {
  return useQuery({
    queryKey: ['CMP', 'privacyNotices'],
    queryFn: fetchPrivacyNotices,
  })
}

export function useSavePrivacyNoticeMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ fieldName, noticeText }: { fieldName: string; noticeText: string }) => savePrivacyNotice(fieldName, noticeText),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['CMP', 'privacyNotices'] }),
  })
}

export function useDeletePrivacyNoticeMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deletePrivacyNotice(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['CMP', 'privacyNotices'] }),
  })
}
