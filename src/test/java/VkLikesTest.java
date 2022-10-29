import com.vk.api.sdk.actions.Likes;
import com.vk.api.sdk.actions.Wall;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiAccessException;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.likes.GetListFriendsOnly;
import com.vk.api.sdk.objects.likes.Type;
import com.vk.api.sdk.objects.likes.responses.AddResponse;
import com.vk.api.sdk.objects.likes.responses.DeleteResponse;
import com.vk.api.sdk.objects.likes.responses.GetListResponse;
import com.vk.api.sdk.objects.likes.responses.IsLikedResponse;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import com.vk.api.sdk.queries.likes.LikesAddQuery;
import com.vk.api.sdk.queries.likes.LikesDeleteQuery;
import com.vk.api.sdk.queries.likes.LikesGetListQuery;
import com.vk.api.sdk.queries.likes.LikesIsLikedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;


public class VkLikesTest extends VkParentTest {
    private static final Logger logger = LoggerFactory.getLogger(VkParentTest.class);

    private final Likes likes = new Likes(client);
    private UserActor actor;

    @BeforeClass
    public void setUp() {
        Optional<UserActor> actor = this.createUserActor(this.client);
        if (actor.isPresent()) {
            this.actor = actor.get();
        } else {
            logger.error("Can't start test without UserActor");
            throw new RuntimeException("Data for UserActor is not provided");
        }
    }

    // with predefined user additional requests to server to receive post id are not required
    private List<WallpostFull> getPosts() throws ClientException, ApiException {
        Wall wall = new Wall(this.client);
        GetResponse wallGetResponse = wall.get(this.actor).execute();
        return wallGetResponse.getItems();
    }

    private List<WallpostFull> getPostsByCondition(Predicate<WallpostFull> predicate) throws ClientException, ApiException {
        return this.getPosts()
                .stream()
                .filter(predicate)
                // for ddt example all posts not needed
                .limit(5)
                .toList();
    }

    private WallpostFull getLastPostByCondition(Predicate<WallpostFull> predicate) throws ClientException, ApiException {
        return this.getPosts()
                .stream()
                .filter(predicate)
                .toList().get(0);
    }

    // Assuming we have fresh user without likes on posts
    // In normal conditions this test should be parametrized by different types of content (post, comment, photo, etc)
    @Test(groups = {"likesAdd"}, dataProvider = "postProvider")
    public void testAddLikeToPost(WallpostFull post) throws ClientException, ApiException {
        int count = post.getLikes().getCount();

        LikesAddQuery likesQuery = this.likes.add(actor, Type.POST, post.getId());
        AddResponse response = likesQuery.execute();

        assertNotNull(response);

        int current = response.getLikes();

        assertEquals("Count after Likes.Add doesn't change", count + 1, current);
    }

    @Test(groups = {"likesAdd"})
    public void testAddLikeToPostWithLike() throws ClientException, ApiException {
        WallpostFull wallpostFull = this.getLastPostByCondition(el -> !el.getLikes().canLike());
        int count = wallpostFull.getLikes().getCount();

        LikesAddQuery likesQuery = this.likes.add(actor, Type.POST, wallpostFull.getId());
        AddResponse response = likesQuery.execute();

        assertNotNull(response);

        int current = response.getLikes();

        assertEquals("Count change after Likes.Add on post with like", count, current);
    }

    @Test(groups = {"likesDelete"})
    public void testDeleteLikeFromPost() throws ClientException, ApiException {
        WallpostFull wallpostFull = this.getLastPostByCondition(el -> !el.getLikes().canLike());
        int count = wallpostFull.getLikes().getCount();

        LikesDeleteQuery delete = this.likes.delete(actor, Type.POST, wallpostFull.getId());
        DeleteResponse response = delete.execute();

        assertNotNull(response);

        int current = response.getLikes();

        assertEquals("Likes count doesn't change after Likes.delete", count - 1, current);
    }

    @Test(groups = {"likesDelete"}, expectedExceptions = ApiAccessException.class)
    public void testDeleteLikeFromPostWithoutLike() throws ClientException, ApiException {
        WallpostFull wallpostFull = this.getLastPostByCondition(el -> el.getLikes().canLike());
        int count = wallpostFull.getLikes().getCount();

        LikesDeleteQuery delete = this.likes.delete(actor, Type.POST, wallpostFull.getId());
        DeleteResponse response = delete.execute();

        assertNotNull(response);

        int current = response.getLikes();

        assertEquals("Likes count change after Likes.delete on post without Like", count, current);
    }

    // Assuming we have user with post with friends/etc likes
    @Test(groups = {"likesList"})
    public void testGetLikeList() throws ClientException, ApiException {
        WallpostFull wallpostFull = this.getLastPostByCondition(el-> el.getLikes().getCount() > 0);
        int expected = wallpostFull.getLikes().getCount();

        LikesGetListQuery getListQuery = this.likes.getList(actor, Type.POST);
        getListQuery.itemId(wallpostFull.getId());
        GetListResponse listResponse = getListQuery.execute();

        assertNotNull(listResponse);

        int current = listResponse.getCount();

        assertEquals("Likes count doesn't match", expected, current);

        assertEquals("Size of Likes.getList response is not equal count", expected, listResponse.getItems().size());
    }

    // One of negative cases for this method - get Likes with owner_id or item_id what belongs to another user with restrictions
    // Second positive case, assuming user have post only with friends likes
    @Test(groups = {"likesList"})
    public void testGetLikeListFriendsOnly() throws ClientException, ApiException {
        WallpostFull wallpostFull = this.getLastPostByCondition(el-> el.getLikes().getCount() == 0);

        LikesGetListQuery getListQuery = this.likes.getList(actor, Type.POST);
        getListQuery.itemId(wallpostFull.getId());
        getListQuery.friendsOnly(GetListFriendsOnly._1);
        GetListResponse listResponse = getListQuery.execute();

        assertNotNull(listResponse);

        assertEquals("Size of Likes.getList response with friends only is not equal count", 0, listResponse.getItems().size());
    }

    @Test(groups = {"islLiked"})
    public void testIsElementLiked() throws ClientException, ApiException {
        WallpostFull wallpostFull = this.getLastPostByCondition(el -> el.getLikes().getUserLikes() > 0);
        int postId = wallpostFull.getId();
        LikesIsLikedQuery liked = this.likes.isLiked(actor, Type.POST, postId);
        IsLikedResponse isLikedResponse = liked.execute();

        assertNotNull(isLikedResponse);

        assertEquals("Likes.isLiked response on liked post should be true", true, isLikedResponse.isLiked());
    }

    @Test(groups = {"islLiked"})
    public void testIsElementLikedOnNonLikedPost() throws ClientException, ApiException {
        WallpostFull wallpostFull = this.getLastPostByCondition(el -> el.getLikes().getUserLikes() == 0);
        int postId = wallpostFull.getId();
        LikesIsLikedQuery liked = this.likes.isLiked(actor, Type.POST, postId);
        IsLikedResponse isLikedResponse = liked.execute();

        assertNotNull(isLikedResponse);

        assertEquals("Likes.isLiked response on non liked post should be false", false, isLikedResponse.isLiked());
    }

    @DataProvider(name = "postProvider")
    public Object[][] getWallData() throws ClientException, ApiException {
        return this.getPostsByCondition(el -> el.getLikes().canLike())
                .stream()
                .map(el -> new Object[]{el})
                .toArray(Object[][]::new);
    }

}
