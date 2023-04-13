# MPEG DASH Program
~~~
Video (+Audio) Live Streaming Servers & Clients

1. The above program, 
    1) As a DASH server, it can receive and process requests from other DASH clients, 
    2) As a DASH client, you can send a request to another DASH server to get the media stream.
    3) Server and client functions do not operate separately from each other, but are interoperable.

2. When operating as a DASH server, 
    1) When importing media streams, [RTMP, DASH] methods are currently available.
    2) Static media stream can be prefetched using DASH method (works as a DASH client)
        Assuming that the > Dynamic Media Stream is published to an RTMP server, 
          Published Media Stream is implemented to get it before the user requests it.
          (Acts as an RTMP client)

3. When operating as a DASH client, 
    1) When transmitting media streams, both video and audio need to be transmitted, > only audio is not transmitted from the Open API used.
        > RTMP requires video + audio, DASH allows video or audio selection
    2) When receiving media streams, only audio can be received separately > (playing), video or audio reception.

4 REFERENCE
  1) Make MPD & Segments
    + REF : org.bytedeco.javacv (static, dynamic)
            (https://github.com/bytedeco/javacv)

  2) MPD class object & Validatation of MPD file
    + REF : carlanton/mpd-tools (io.linstrom:mpd-parser)
            (https://github.com/carlanton/mpd-tools)

  3) Camera
    + REF : org.bytedeco.javacv
            (https://github.com/bytedeco/javacv)

~~~
## MAIN CODE LOGIC
! [JDASH_Main Code Logic] (https://user-images.githubusercontent.com/37236920/176051458-83c6e08c-20d5-4c5f-b63c-a7ed552dc485.png)
  
## Service
! [dash_stream_flow] (https://user-images.githubusercontent.com/37236920/159614833-43d3128c-fadb-435f-a528-80d496463b57.png)
  
  
! [Screenshot 2/15/2022 4 02 51 PM] (https://user-images.githubusercontent.com/37236920/154009715-e31fbbd9-d4b9-489d-93ed-ec72d3c00b1a.png)
  
! [Screenshot 2/10/2022 9 40 31 AM] (https://user-images.githubusercontent.com/37236920/153314792-6cc61897-911f-4924-a8fc-79ce2cf6131a.png)
  
### (1) Before DRM is applied
! [Screenshot 2/9/2022 3 47 00 PM] (https://user-images.githubusercontent.com/37236920/153136606-7c5bbc7c-249f-4b8d-a3ea-3b73cc8277ae.png)
  
### (2) After DRM is applied
! [Screenshot 2/9/2022 3 47 20 PM] (https://user-images.githubusercontent.com/37236920/153136655-ae0c1257-ba93-4c56-b355-5c22eae7b844.png)
  
## Flow
### (1) Before DRM is applied
! [Screenshot 2/9/2022 3 44 54 PM] (https://user-images.githubusercontent.com/37236920/153136334-78c4ca9a-ef10-42f1-bcea-40a263869f1c.png)
  
### (2) After DRM is applied
! [Screenshot 2/9/2022 3 45 54 PM] (https://user-images.githubusercontent.com/37236920/153136472-932d3a75-a20f-452f-b31e-6d7a2e9b2929.png)
  
## Data structure
! [Screenshot 2/04/2022 9 31 14 AM] (https://user-images.githubusercontent.com/37236920/152452171-363bed03-416d-433a-85d5-b85c394b1ff4.png)
  
