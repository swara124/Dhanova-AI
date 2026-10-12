package com.example.dhanovaai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PATCH;


public interface ApiService {
    @POST("register")
    Call<AuthResponse> registerUser(@Body RegisterRequest request);

    @POST("login")
    Call<AuthResponse> loginUser(@Body LoginRequest request);

    @POST("verify-pin")
    Call<AuthResponse> verifyPin(@Body PinRequest request);

    @POST("family/create")
    Call<CreateFamilyResponse> createFamily(@Body CreateFamilyRequest request);

    @POST("family/invite")
    Call<SimpleMessageResponse> inviteMember(@Body InviteRequest request);

    @GET("family/invitations")
    Call<InvitationsResponse> getInvitations();

    @POST("family/invitations/{id}/respond")
    Call<SimpleMessageResponse> respondInvitation(@Path("id") int invitationId, @Body RespondRequest request);

    @GET("family/{familyId}/members")
    Call<MembersResponse> getFamilyMembers(@Path("familyId") int familyId);

    @GET("family/{familyId}/members/{memberId}/permissions")
    Call<PermissionsResponse> getMemberPermissions(@Path("familyId") int familyId, @Path("memberId") int memberId);

    @PUT("family/{familyId}/members/{memberId}/permissions")
    Call<SimpleMessageResponse> setMemberPermissions(@Path("familyId") int familyId, @Path("memberId") int memberId, @Body SetPermissionsRequest request);

    @GET("family/my-families")
    Call<MyFamiliesResponse> getMyFamilies();

    @POST("family/{familyId}/transactions")
    Call<SimpleMessageResponse> addTransaction(@Path("familyId") int familyId, @Body AddTransactionRequest request);

    @PATCH("family/{familyId}/members/{memberId}/role")
    Call<SimpleMessageResponse> setMemberRole(@Path("familyId") int familyId, @Path("memberId") int memberId, @Body RoleRequest request);

    @GET("transactions/summary")
    Call<SummaryResponse> getSummary();
}