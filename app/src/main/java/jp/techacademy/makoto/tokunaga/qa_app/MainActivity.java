package jp.techacademy.makoto.tokunaga.qa_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.support.v7.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Toolbar mToolbar;
    private int mGenre = 0;

    // --- ここから ---
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mGenreRef;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private QuestionsListAdapter mAdapter;

    private Map<String, String> mMap;

    private NavigationView mNavigationView;
    private List<String> mList;
    private Menu mMenu;
    private MenuItem mStar;

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();
            String title = (String) map.get("title");
            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");
            String imageString = (String) map.get("image");
            byte[] bytes;
            if (imageString != null) {
                bytes = Base64.decode(imageString, Base64.DEFAULT);
            } else {
                bytes = new byte[0];
            }

            ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
            HashMap answerMap = (HashMap) map.get("answers");
            if (answerMap != null) {
                for (Object key : answerMap.keySet()) {
                    HashMap temp = (HashMap) answerMap.get((String) key);
                    String answerBody = (String) temp.get("body");
                    String answerName = (String) temp.get("name");
                    String answerUid = (String) temp.get("uid");
                    Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                    answerArrayList.add(answer);
                }
            }

            Question question = new Question(title, body, name, uid, dataSnapshot.getKey(), mGenre, bytes, answerArrayList);
            mQuestionArrayList.add(question);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            // 変更があったQuestionを探す
            for (Question question : mQuestionArrayList) {
                if (dataSnapshot.getKey().equals(question.getQuestionUid())) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.getAnswers().clear();
                    HashMap answerMap = (HashMap) map.get("answers");
                    if (answerMap != null) {
                        for (Object key : answerMap.keySet()) {
                            HashMap temp = (HashMap) answerMap.get((String) key);
                            String answerBody = (String) temp.get("body");
                            String answerName = (String) temp.get("name");
                            String answerUid = (String) temp.get("uid");
                            Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                            question.getAnswers().add(answer);
                        }
                    }

                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    };

    // --- ここまで追加する ---
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        //+---------------------------------------------------------------------------------------------+
        mMap = new HashMap<String, String>();
        mList = new ArrayList<String>();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        mMenu = mNavigationView.getMenu();
        mStar = mMenu.findItem(R.id.nav_favstar);
        //+---------------------------------------------------------------------------------------------+

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
                if (mGenre == 0) {
                    Snackbar.make(view, "ジャンルを選択して下さい", Snackbar.LENGTH_LONG).show();
                    return;
                }

                // ログイン済みのユーザーを取得する
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user == null) {
                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // ジャンルを渡して質問作成画面を起動する
                    Intent intent = new Intent(getApplicationContext(), QuestionSendActivity.class);
                    intent.putExtra("genre", mGenre);
                    startActivity(intent);
                }

            }
        });

        // ナビゲーションドロワーの設定
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        setMenuBar();


        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionsListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mAdapter.notifyDataSetChanged();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Questionのインスタンスを渡して質問詳細画面を起動する
                Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                intent.putExtra("question", mQuestionArrayList.get(position));
                startActivity(intent);
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();

        // 1:趣味を既定の選択とする
        if (mGenre == 0) {
            mNavigationView = (NavigationView) findViewById(R.id.nav_view);
            onNavigationItemSelected(mNavigationView.getMenu().getItem(0));
        }
        // --- ここから ---
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear();
        mAdapter.setQuestionArrayList(mQuestionArrayList);
        mListView.setAdapter(mAdapter);

        // 選択したジャンルにリスナーを登録する
        if (mGenreRef != null) {
            mGenreRef.removeEventListener(mEventListener);
        }

        if (mGenre != 5) {
            mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
            mGenreRef.addChildEventListener(mEventListener);
        } else {
            getmFavoriteList();
        }

        setMenuBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_hobby) {
            mToolbar.setTitle("趣味");
            mGenre = 1;
        } else if (id == R.id.nav_life) {
            mToolbar.setTitle("生活");
            mGenre = 2;
        } else if (id == R.id.nav_health) {
            mToolbar.setTitle("健康");
            mGenre = 3;
        } else if (id == R.id.nav_compter) {
            mToolbar.setTitle("コンピューター");
            mGenre = 4;
        } else if (id == R.id.nav_favstar) {
            mToolbar.setTitle("お気に入り");
            mGenre = 5;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        // --- ここから ---
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear();
        mAdapter.setQuestionArrayList(mQuestionArrayList);
        mListView.setAdapter(mAdapter);

        // 選択したジャンルにリスナーを登録する
        if (mGenreRef != null) {
            mGenreRef.removeEventListener(mEventListener);
        }

        if (mGenre != 5) {
            mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
            mGenreRef.addChildEventListener(mEventListener);
        } else {
            getmFavoriteList();
        }
        // --- ここまで追加する ---
        return true;
    }

    public void setMenuBar() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            mStar.setVisible(false);
        } else {
            mStar.setVisible(true);
        }
    }

    private void getmFavoriteList() {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("metauid", myUid);

        if (myUid != null) {

            DatabaseReference tmpFD = FirebaseDatabase.getInstance().getReference();
            final DatabaseReference favRef = tmpFD.child(Const.FavoritePATH).child(myUid);
            Log.d("metafavref", String.valueOf(favRef));

            favRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Map<String, String> tmpMap = new HashMap<String, String>();
                    Log.d("metavalue", String.valueOf(dataSnapshot.getValue()));
                    tmpMap = (HashMap<String, String>) dataSnapshot.getValue();
                    mMap.put(dataSnapshot.getKey(), tmpMap.get(Const.GenrePath));
                    Log.d("metaConst",String.valueOf(dataSnapshot.getValue()));
                    Log.d("metaConst",String.valueOf(tmpMap.get(Const.GenrePath)));

                    mMap.remove("First Commit");
                    Log.d("metamap2", String.valueOf(mMap));
                    Log.d("metaConstr",String.valueOf(dataSnapshot.getValue()));
                    Log.d("metaConstr",String.valueOf(tmpMap.get(Const.GenrePath)));

                    if(dataSnapshot.getKey() != null) {

                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                        DatabaseReference targetRef = databaseReference.child(Const.ContentsPATH).child(String.valueOf(tmpMap.get(Const.GenrePath))).child(dataSnapshot.getKey());
                        targetRef
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                        if(!dataSnapshot.getKey().equals("First Commit")) {

                                            Log.d("innerclass", String.valueOf(dataSnapshot));

                                            HashMap map = (HashMap) dataSnapshot.getValue();
                                            String title = (String) map.get("title");
                                            String body = (String) map.get("body");
                                            String name = (String) map.get("name");
                                            String uid = (String) map.get("uid");
                                            String imageString = (String) map.get("image");
                                            String genre = (String) map.get(Const.GenrePath);
                                            byte[] bytes;
                                            if (imageString != null) {
                                                bytes = Base64.decode(imageString, Base64.DEFAULT);
                                            } else {
                                                bytes = new byte[0];
                                            }

                                            ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
                                            HashMap answerMap = (HashMap) map.get("answers");
                                            if (answerMap != null) {
                                                for (Object key : answerMap.keySet()) {
                                                    HashMap temp = (HashMap) answerMap.get((String) key);
                                                    String answerBody = (String) temp.get("body");
                                                    String answerName = (String) temp.get("name");
                                                    String answerUid = (String) temp.get("uid");
                                                    Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                                                    answerArrayList.add(answer);
                                                }
                                            }

                                            Question question = new Question(title, body, name, uid, dataSnapshot.getKey(), Integer.valueOf(genre), bytes, answerArrayList);
                                            mQuestionArrayList.add(question);
                                            mAdapter.notifyDataSetChanged();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) { }
                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Map<String, String> tmpMap = new HashMap<String, String>();
                    Log.d("metavalue", String.valueOf(dataSnapshot.getValue().getClass()));
                    tmpMap = (HashMap<String, String>) dataSnapshot.getValue();
                    mMap.put(dataSnapshot.getKey(), tmpMap.get(Const.GenrePath));
                    mMap.remove("First Commit");


                    if(dataSnapshot.getKey()!= null) {

                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                        DatabaseReference targetRef = databaseReference.child(Const.ContentsPATH).child(String.valueOf(tmpMap.get(Const.GenrePath))).child(dataSnapshot.getKey());
                        targetRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                Log.d("innerclass", String.valueOf(dataSnapshot));
                                if(!dataSnapshot.getKey().equals("First Commit")) {


                                    HashMap map = (HashMap) dataSnapshot.getValue();
                                String title = (String) map.get("title");
                                String body = (String) map.get("body");
                                String name = (String) map.get("name");
                                String uid = (String) map.get("uid");
                                String imageString = (String) map.get("image");
                                String genre = (String) map.get(Const.GenrePath);
                                byte[] bytes;
                                if (imageString != null) {
                                    bytes = Base64.decode(imageString, Base64.DEFAULT);
                                } else {
                                    bytes = new byte[0];
                                }

                                ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
                                HashMap answerMap = (HashMap) map.get("answers");
                                if (answerMap != null) {
                                    for (Object key : answerMap.keySet()) {
                                        HashMap temp = (HashMap) answerMap.get((String) key);
                                        String answerBody = (String) temp.get("body");
                                        String answerName = (String) temp.get("name");
                                        String answerUid = (String) temp.get("uid");
                                        Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                                        answerArrayList.add(answer);
                                    }
                                }

                                Question question = new Question(title, body, name, uid, dataSnapshot.getKey(), Integer.valueOf(genre), bytes, answerArrayList);
                                mQuestionArrayList.remove(question);
                                mAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                            }
                        });
                    }
                }
                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) { }
                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        }
    }
}
