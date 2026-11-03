package com.example.spliteasy.api;

import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/register")
    Call<ResponseBody> registerUser(@Body RegisterRequest request);

    @POST("api/login")
    Call<LoginResponse> loginUser(@Body LoginRequest request);

    @POST("api/groups/create")
    Call<Group> createGroup(@Body CreateGroupRequest request);

    @DELETE("api/groups/{groupId}")
    Call<ResponseBody> deleteGroup(@Path("groupId") Long groupId, @Query("userId") Long userId);

    @DELETE("api/groups/announcements/{announcementId}")
    Call<ResponseBody> deleteAnnouncement(@Path("announcementId") Long announcementId, @Query("userId") Long userId);

    @DELETE("api/chores/{choreId}")
    Call<ResponseBody> deleteChore(@Path("choreId") Long choreId, @Query("userId") Long userId);

    @DELETE("api/groups/subscriptions/{subscriptionId}")
    Call<ResponseBody> deleteSubscription(@Path("subscriptionId") Long subscriptionId, @Query("userId") Long userId);

    @DELETE("api/groups/documents/{documentId}")
    Call<ResponseBody> deleteDocument(@Path("documentId") Long documentId, @Query("userId") Long userId);

    @GET("api/groups/user/{userId}")
    Call<List<Group>> getGroupsForUser(@Path("userId") Long userId);

    @POST("api/groups/{groupId}/members/add")
    Call<UserDto> addMemberByEmail(@Path("groupId") Long groupId, @Body AddMemberRequest request);

    @DELETE("api/groups/{groupId}/members/{memberUserId}")
    Call<ResponseBody> removeMember(@Path("groupId") Long groupId, @Path("memberUserId") Long memberUserId, @Query("requesterUserId") Long requesterUserId);

    @GET("api/groups/{groupId}/members")
    Call<List<UserDto>> getGroupMembers(@Path("groupId") Long groupId);

    @POST("api/groups/{groupId}/expenses/create")
    Call<Expense> createExpense(@Path("groupId") Long groupId, @Body CreateExpenseRequest request);

    @GET("api/groups/{groupId}/expenses")
    Call<List<Expense>> getGroupExpenses(@Path("groupId") Long groupId);

    @GET("api/groups/{groupId}/balances")
    Call<BalanceResponse> getGroupBalances(@Path("groupId") Long groupId);

    @POST("api/groups/{groupId}/settlements/record")
    Call<Settlement> recordSettlement(@Path("groupId") Long groupId, @Body RecordSettlementRequest request);

    @GET("api/groups/{groupId}/settlements")
    Call<List<Settlement>> getGroupSettlements(@Path("groupId") Long groupId);

    @POST("api/pin/verify")
    Call<ResponseBody> verifyPin(@Body PinVerifyRequest request);

    @POST("api/pin/set")
    Call<ResponseBody> setPin(@Body PinSetRequest request);

    @POST("api/pin/recovery/request-otp")
    Call<ResponseBody> requestOtp(@Body RecoveryRequest request);

    @POST("api/pin/recovery/reset")
    Call<ResponseBody> resetPin(@Body PinResetRequest request);

    @POST("api/user/upi")
    Call<ResponseBody> updateUpiId(@Body UpiUpdateRequest request);

    @POST("api/user/birthday")
    Call<ResponseBody> updateBirthday(@Body BirthdayRequest request);

    @POST("api/groups/{groupId}/remind")
    Call<ResponseBody> sendReminder(@Path("groupId") Long groupId, @Body RemindRequest request);

    @GET("api/groups/{groupId}/reminders/status")
    Call<List<PaymentReminder>> getGroupRemindersStatus(@Path("groupId") Long groupId, @Query("senderUserId") Long senderUserId);

    @GET("api/reminders/user/{userId}")
    Call<List<PaymentReminder>> getRemindersForUser(@Path("userId") Long userId);

    @POST("api/reminders/{reminderId}/view")
    Call<ResponseBody> markReminderViewed(@Path("reminderId") Long reminderId);

    @GET("api/groups/expenses/{expenseId}/receipt")
    Call<okhttp3.ResponseBody> getExpenseReceipt(@Path("expenseId") Long expenseId, @Query("userId") Long userId);

    @GET("api/groups/{groupId}/insights")
    Call<GroupInsightsResponse> getGroupInsights(@Path("groupId") Long groupId);

    @POST("api/groups/{groupId}/announcements/create")
    Call<GroupAnnouncement> createAnnouncement(@Path("groupId") Long groupId, @Body AnnouncementRequest request);

    @GET("api/groups/{groupId}/announcements")
    Call<List<GroupAnnouncement>> getAnnouncements(@Path("groupId") Long groupId, @Query("userId") Long userId);

    @POST("api/groups/{groupId}/subscriptions/create")
    Call<GroupSubscription> createSubscription(@Path("groupId") Long groupId, @Body SubscriptionRequest request);

    @GET("api/groups/{groupId}/subscriptions")
    Call<List<GroupSubscription>> getSubscriptions(@Path("groupId") Long groupId, @Query("userId") Long userId);

    @GET("api/groups/{groupId}/birthdays")
    Call<List<Map<String, Object>>> getGroupBirthdays(@Path("groupId") Long groupId, @Query("userId") Long userId);

    // Trip Planner & Budget
    @POST("api/groups/{groupId}/trips/create")
    Call<Trip> createTrip(@Path("groupId") Long groupId, @Body TripRequest request);

    @GET("api/groups/{groupId}/trips")
    Call<List<Trip>> getTrips(@Path("groupId") Long groupId, @Query("userId") Long userId);

    @PUT("api/groups/{groupId}/trips/{tripId}")
    Call<Trip> updateTrip(@Path("groupId") Long groupId, @Path("tripId") Long tripId, @Body TripRequest request);

    @DELETE("api/groups/{groupId}/trips/{tripId}")
    Call<ResponseBody> deleteTrip(@Path("groupId") Long groupId, @Path("tripId") Long tripId, @Query("userId") Long userId);

    @POST("api/groups/{groupId}/trips/{tripId}/itinerary/add")
    Call<TripItinerary> addItinerary(@Path("groupId") Long groupId, @Path("tripId") Long tripId, @Body ItineraryRequest request);

    @GET("api/groups/{groupId}/trips/{tripId}/itinerary")
    Call<List<TripItinerary>> getItinerary(@Path("groupId") Long groupId, @Path("tripId") Long tripId, @Query("userId") Long userId);

    @DELETE("api/groups/{groupId}/trips/itinerary/{itemId}")
    Call<ResponseBody> deleteItinerary(@Path("groupId") Long groupId, @Path("itemId") Long itemId, @Query("userId") Long userId);

    @POST("api/groups/{groupId}/trips/{tripId}/expenses/add")
    Call<TripExpense> addTripExpense(@Path("groupId") Long groupId, @Path("tripId") Long tripId, @Body TripExpenseRequest request);

    @GET("api/groups/{groupId}/trips/{tripId}/expenses")
    Call<List<TripExpense>> getTripExpenses(@Path("groupId") Long groupId, @Path("tripId") Long tripId, @Query("userId") Long userId);

    @DELETE("api/groups/{groupId}/trips/expenses/{expenseId}")
    Call<ResponseBody> deleteTripExpense(@Path("groupId") Long groupId, @Path("expenseId") Long expenseId, @Query("userId") Long userId);

    // Party & Event Planner
    @POST("api/groups/{groupId}/events/create")
    Call<GroupEvent> createEvent(@Path("groupId") Long groupId, @Body EventRequest request);

    @GET("api/groups/{groupId}/events")
    Call<List<GroupEvent>> getEvents(@Path("groupId") Long groupId, @Query("userId") Long userId);

    @PUT("api/groups/{groupId}/events/{eventId}")
    Call<GroupEvent> updateEvent(@Path("groupId") Long groupId, @Path("eventId") Long eventId, @Body EventRequest request);

    @DELETE("api/groups/{groupId}/events/{eventId}")
    Call<ResponseBody> deleteEvent(@Path("groupId") Long groupId, @Path("eventId") Long eventId, @Query("userId") Long userId);

    // Guest List & RSVP
    @POST("api/groups/{groupId}/events/{eventId}/guests/add")
    Call<EventGuest> addGuest(@Path("groupId") Long groupId, @Path("eventId") Long eventId, @Body GuestRequest request);

    @GET("api/groups/{groupId}/events/{eventId}/guests")
    Call<List<EventGuest>> getGuests(@Path("groupId") Long groupId, @Path("eventId") Long eventId, @Query("userId") Long userId);

    @PUT("api/groups/{groupId}/events/{eventId}/guests/{guestId}/status")
    Call<EventGuest> updateRsvpStatus(@Path("groupId") Long groupId, @Path("eventId") Long eventId, @Path("guestId") Long guestId, @Body GuestRequest request);

    @DELETE("api/groups/{groupId}/events/{eventId}/guests/{guestId}")
    Call<ResponseBody> deleteGuest(@Path("groupId") Long groupId, @Path("eventId") Long eventId, @Path("guestId") Long guestId, @Query("userId") Long userId);

    // Shared Calendar & Polls
    @POST("api/groups/{groupId}/calendar/create")
    Call<CalendarEntry> createCalendarEntry(@Path("groupId") Long groupId, @Body CalendarRequest request);

    @GET("api/groups/{groupId}/calendar")
    Call<List<CalendarEntry>> getCalendarEntries(@Path("groupId") Long groupId, @Query("userId") Long userId);

    @DELETE("api/groups/{groupId}/calendar/{entryId}")
    Call<ResponseBody> deleteCalendarEntry(@Path("groupId") Long groupId, @Path("entryId") Long entryId, @Query("userId") Long userId);

    @POST("api/groups/{groupId}/polls/create")
    Call<Poll> createPoll(@Path("groupId") Long groupId, @Body PollRequest request);

    @GET("api/groups/{groupId}/polls")
    Call<List<Poll>> getPolls(@Path("groupId") Long groupId, @Query("userId") Long userId);

    @POST("api/groups/{groupId}/polls/{pollId}/vote")
    Call<ResponseBody> votePoll(@Path("groupId") Long groupId, @Path("pollId") Long pollId, @Body VoteRequest request);

    @DELETE("api/groups/{groupId}/polls/{pollId}")
    Call<ResponseBody> deletePoll(@Path("groupId") Long groupId, @Path("pollId") Long pollId, @Query("userId") Long userId);

    // Shared Shopping List
    @POST("api/groups/{groupId}/shopping/add")
    Call<ShoppingItem> addShoppingItem(@Path("groupId") Long groupId, @Body ShoppingRequest request);

    @GET("api/groups/{groupId}/shopping")
    Call<List<ShoppingItem>> getShoppingItems(@Path("groupId") Long groupId, @Query("userId") Long userId);

    @PUT("api/groups/{groupId}/shopping/{itemId}/status")
    Call<ShoppingItem> updateShoppingItemStatus(@Path("groupId") Long groupId, @Path("itemId") Long itemId, @Body ShoppingRequest request);

    @DELETE("api/groups/{groupId}/shopping/{itemId}")
    Call<ResponseBody> deleteShoppingItem(@Path("groupId") Long groupId, @Path("itemId") Long itemId, @Query("userId") Long userId);
}
