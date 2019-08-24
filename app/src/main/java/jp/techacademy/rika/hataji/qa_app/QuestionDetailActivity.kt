package jp.techacademy.rika.hataji.qa_app

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ListView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity(){

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoritesRef: DatabaseReference

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)


        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            favorite_button.setVisibility(View.GONE) // 非表示（ボタンがあったところのスペースは詰める）



        } else {
            favorite_button.setVisibility(View.VISIBLE) // 表示

            ///データ読み取り
            mFavoritesRef = dataBaseReference.child(FavoritesPATH).child(user!!.uid)
            var favoriteRef = mFavoritesRef.child(FavoritesPATH).child(user!!.uid).child(mQuestion!!.questionUid)
            Log.d("ANDROID", user!!.uid)
            favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Snapshot(DBにデータがあるかどうか代入するオブジェクト)
                    val data = snapshot.value as Map<*, *>?

                    //firebaseに登録されているかチェック
                    if(data == null){
                        //未登録
                        favorite_button.text = getString(R.string.favorite_register)
                    } else {
                        //登録済
                        favorite_button.text = getString(R.string.favorite_registered)
                    }
                }
                override fun onCancelled(firebaseError: DatabaseError) {}
            })

            //お気に入りボタンリスナー
            favorite_button.setOnClickListener {
                if(favorite_button.text == getString(R.string.favorite_register)) {
                    //登録する
                    favorite_button.text = getString(R.string.favorite_registered)
                    val favoritesRef = dataBaseReference.child(FavoritesPATH).child(user!!.uid).child(mQuestion.questionUid)

                    //firebaseに保存
                    val data = HashMap<String, String>()
                    data["qid"] = mQuestion.questionUid
                    data["genre"] = mQuestion.genre.toString()
                    favoritesRef.setValue(data)
                } else {
                    //登録解除
                    favorite_button.text = getString(R.string.favorite_register)
                    val favoritesRef = dataBaseReference.child(FavoritesPATH).child(user!!.uid).child(mQuestion.questionUid)

                    //firebaseにから削除
                    favoritesRef.removeValue()
                }
            }


        }

    }

}