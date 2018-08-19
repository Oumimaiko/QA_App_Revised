package jp.techacademy.makoto.tokunaga.qa_app;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class QuestionDetailActivity extends AppCompatActivity {

    private ListView mListView;
    private Question mQuestion;
    private QuestionDetailListAdapter mAdapter;

    private DatabaseReference mAnswerRef;

    private FloatingActionButton mFavButton;

    private boolean mflag = false;

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            String answerUid = dataSnapshot.getKey();

            for(Answer answer : mQuestion.getAnswers()) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid.equals(answer.getAnswerUid())) {
                    return;
                }
            }

            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");

            Answer answer = new Answer(body, name, uid, answerUid);
            mQuestion.getAnswers().add(answer);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        // 渡ってきたQuestionのオブジェクトを保持する
        Bundle extras = getIntent().getExtras();
        mQuestion = (Question) extras.get("question");

        setTitle(mQuestion.getTitle());

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionDetailListAdapter(this, mQuestion);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // ログイン済みのユーザーを取得する
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user == null) {
                    // ログインしていなければログイン画面に遷移させる
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    // Questionを渡して回答作成画面を起動する
                    // --- ここから ---
                    Intent intent = new Intent(getApplicationContext(), AnswerSendActivity.class);
                    intent.putExtra("question", mQuestion);
                    startActivity(intent);
                    // --- ここまで ---
                }
            }
        });

        // +----------------------------------------------------------------------------------------+
        //お気に入りフラグ判定
        String myUid = getMyUid();
        DatabaseReference tmpDR = FirebaseDatabase.getInstance().getReference();
        tmpDR.child(Const.FavoritePATH)
                .child(myUid)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Log.d("metaAndroidDataSnapshot",String.valueOf(dataSnapshot));
                        Log.d("metaAndroidDataSnapshot",String.valueOf(s));
                        if(mQuestion.getQuestionUid().equals(s)){
                            mflag = true;
                            mFavButton.setImageResource(R.drawable.outline_star_white_24dp);
                        }
                    }
                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) { }
                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        /*
                        Log.d("metaAndroidRemoved",String.valueOf(dataSnapshot));
                        if(mQuestion.getQuestionUid().equals(dataSnapshot.getKey())){
                            mflag = true;
                            mFavButton.setImageResource(R.drawable.outline_star_white_24dp);

                        } */
                    }
                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) { }
                    @Override
                    public void onCancelled(DatabaseError databaseError) { }
                });


        //お気に入りボタンの実装
        mFavButton = (FloatingActionButton)findViewById(R.id.favstar);
        findViewById(R.id.favstar).setVisibility(View.VISIBLE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            findViewById(R.id.favstar).setVisibility(View.INVISIBLE);
        } else {
            findViewById(R.id.favstar).setVisibility(View.VISIBLE);
        }

        mFavButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mflag){
                    mFavButton.setImageResource(R.drawable.outline_star_border_white_24dp);
                    mflag = false;
                    String myUid = getMyUid();
                    DatabaseReference tmpDR = FirebaseDatabase.getInstance().getReference();
                    tmpDR.child(Const.FavoritePATH).child(myUid).child(mQuestion.getQuestionUid()).removeValue();
                } else {
                    mFavButton.setImageResource(R.drawable.outline_star_white_24dp);
                    mflag = true;

                    String myUid = getMyUid();
                    Map<String,String> tmpMap = new HashMap<String,String>();
                    tmpMap.put(Const.GenrePath,String.valueOf(mQuestion.getGenre()));

                    DatabaseReference tmpDR = FirebaseDatabase.getInstance().getReference();
                    tmpDR.child(Const.FavoritePATH).child(myUid).child(mQuestion.getQuestionUid()).setValue(tmpMap);
                }

            }
        });
        // +----------------------------------------------------------------------------------------+

        DatabaseReference dataBaseReference = FirebaseDatabase.getInstance().getReference();
        mAnswerRef = dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.AnswersPATH);
        mAnswerRef.addChildEventListener(mEventListener);

    }

    // +----------------------------------------------------------------------------------------+
    //自分のUidとるためのメソッド
    private String getMyUid(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user.getUid();
    }
    // +----------------------------------------------------------------------------------------+

}

